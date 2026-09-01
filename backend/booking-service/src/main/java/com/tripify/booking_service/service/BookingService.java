package com.tripify.booking_service.service;

import com.tripify.booking_service.client.CatalogClient;
import com.tripify.booking_service.client.UserAuthClient;
import com.tripify.booking_service.dto.AuditLogEntryDTO;
import com.tripify.booking_service.dto.BookingLineDTO;
import com.tripify.booking_service.dto.BookingResponseDTO;
import com.tripify.booking_service.dto.PassengerRequestDTO;
import com.tripify.booking_service.dto.PassengerResponseDTO;
import com.tripify.booking_service.dto.CatalogItemSummaryDTO;
import com.tripify.booking_service.dto.ReceivedBookingLineDTO;
import com.tripify.booking_service.entity.*;
import com.tripify.booking_service.exception.AccessDeniedException;
import com.tripify.booking_service.exception.EmptyCartException;
import com.tripify.booking_service.exception.InvalidBookingStateException;
import com.tripify.booking_service.exception.PaymentValidationException;
import com.tripify.booking_service.exception.ResourceNotFoundException;
import com.tripify.booking_service.messaging.BookingEventPublisher;
import com.tripify.booking_service.repository.*;
import com.tripify.booking_service.messaging.BookingNotificationEvent;
import feign.FeignException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingLineRepository bookingLineRepository;
    private final PassengerRepository passengerRepository;
    private final ShoppingCartService cartService;
    private final BookingAuditService auditService;
    private final CatalogClient catalogClient;
    private final UserAuthClient userAuthClient;
    private final PaymentService paymentService;
    private final BookingEventPublisher eventPublisher;
    private final RabbitTemplate rabbitTemplate;
    private final PlatformTransactionManager transactionManager;

    @Transactional
    public BookingResponseDTO checkout(String userId) {
        return checkout(userId, null, null);
    }

    @Transactional
    public BookingResponseDTO checkout(String userId, List<Long> selectedCartItemIds) {
        return checkout(userId, selectedCartItemIds, null);
    }

    // 1. Processo di Checkout: converte il carrello (o solo gli articoli
    // selezionati) in una Booking confermata. selectedCartItemIds nullo o
    // vuoto = tutto il carrello; altrimenti solo quegli articoli, lasciando
    // gli altri nel carrello per un checkout successivo. idempotencyKey (header
    // Idempotency-Key, opzionale) riconosce il retry di un checkout già andato
    // a buon fine: se una Booking con la stessa chiave esiste già, la
    // restituiamo così com'è invece di crearne una seconda (vedi commento su
    // Booking.idempotencyKey).
    @Transactional
    public BookingResponseDTO checkout(String userId, List<Long> selectedCartItemIds, String idempotencyKey) {
        boolean hasIdempotencyKey = idempotencyKey != null && !idempotencyKey.isBlank();

        if (hasIdempotencyKey) {
            Optional<Booking> alreadyDone = bookingRepository.findByIdempotencyKey(idempotencyKey);
            if (alreadyDone.isPresent()) {
                return toResponseDTO(alreadyDone.get(), userId);
            }
        }

        // getCartForCheckout (non getCartForUser): prende il lock pessimistico
        // sulla riga del carrello per la durata di questa transazione, così un
        // secondo checkout concorrente sullo stesso utente resta in attesa
        // invece di leggere in parallelo lo stesso carrello pieno (vedi
        // ShoppingCartService.getCartForCheckout).
        ShoppingCart cart = cartService.getCartForCheckout(userId);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new EmptyCartException("Impossibile fare il checkout: il carrello è vuoto!");
        }

        List<CartItem> itemsToCheckout = (selectedCartItemIds == null || selectedCartItemIds.isEmpty())
                ? new ArrayList<>(cart.getItems())
                : cart.getItems().stream()
                        .filter(item -> selectedCartItemIds.contains(item.getId()))
                        .collect(Collectors.toList());

        if (itemsToCheckout.isEmpty()) {
            throw new EmptyCartException("Nessuno degli articoli selezionati è presente nel carrello.");
        }

        // Calcoliamo il totale complessivo degli elementi selezionati (BigDecimal, niente errori di arrotondamento)
        BigDecimal totalAmount = itemsToCheckout.stream()
                .map(item -> item.getPriceAtAdded().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Creiamo la testata della prenotazione (inizialmente in stato PENDING)
        // createdAt/updatedAt li scrive Hibernate da solo (@CreationTimestamp/@UpdateTimestamp)
        Booking booking = Booking.builder()
                .userId(userId)
                .totalAmount(totalAmount)
                .bookingDate(LocalDateTime.now())
                .status(BookingStatus.PENDING)
                .idempotencyKey(hasIdempotencyKey ? idempotencyKey : null)
                .lines(new ArrayList<>())
                .build();

        Booking savedBooking;
        try {
            savedBooking = bookingRepository.save(booking);
        } catch (DataIntegrityViolationException ex) {
            // Un'altra richiesta con la stessa Idempotency-Key ha vinto la corsa
            // e ha già salvato la sua Booking tra il controllo sopra e questo
            // save (constraint unique su idempotencyKey): non è un errore reale,
            // restituiamo quella Booking invece di far fallire questa richiesta.
            if (hasIdempotencyKey) {
                return bookingRepository.findByIdempotencyKey(idempotencyKey)
                        .map(existing -> toResponseDTO(existing, userId))
                        .orElseThrow(() -> ex);
            }
            throw ex;
        }

        // Trasformiamo i CartItem in BookingLine, portando avanti anche
        // quantity e l'eventuale hold aperto su catalog-service: da qui in poi
        // è la Booking (non più il carrello) a "possedere" quell'hold, che
        // verrà confermato o rilasciato in base all'esito del pagamento
        // (vedi confirmPayment/cancelBooking).
        List<BookingLine> lines = new ArrayList<>();
        for (CartItem item : itemsToCheckout) {
            BookingLine line = BookingLine.builder()
                    .booking(savedBooking)
                    .catalogItemId(item.getCatalogItemId())
                    .price(item.getPriceAtAdded())
                    .quantity(item.getQuantity())
                    .roomTypeId(item.getRoomTypeId())
                    .fareClassId(item.getFareClassId())
                    .checkIn(item.getCheckIn())
                    .checkOut(item.getCheckOut())
                    .holdId(item.getHoldId())
                    .passengers(new ArrayList<>())
                    .build();

            // I passeggeri (con relativo documento) si aggiungono dopo il checkout
            // tramite addPassenger(), non qui: al momento del checkout sappiamo
            // solo cosa è stato acquistato, non ancora chi viaggerà su ogni riga.
            lines.add(line);
        }

        // savedBooking.getLines().addAll(...) e non setLines(...): "lines" ha
        // orphanRemoval=true, e Hibernate traccia già quella collection (vuota,
        // creata dal builder) da quando l'entità è diventata gestita col save()
        // sopra - sostituirla con una lista nuova fa perdere quel collegamento
        // e Hibernate rifiuta il flush ("A collection with orphan deletion was
        // no longer referenced by the owning entity instance").
        // bookingLineRepository.saveAll(...) (non un secondo bookingRepository.save
        // sulla Booking) evita di richiamare save() su un'entità già salvata in
        // precedenza: essendo ormai "non nuova", passerebbe per entityManager.merge()
        // invece di persist(), inutilmente più complesso per questo caso.
        savedBooking.getLines().addAll(lines);
        bookingLineRepository.saveAll(lines);

        // Evento di audit: creazione della prenotazione. Stessa transazione del
        // salvataggio sopra, quindi se qualcosa fallisce dopo, anche il log
        // viene annullato insieme al resto (nessun log "orfano" di eventi mai accaduti).
        auditService.log(savedBooking, userId, AuditAction.CREATED,
                "Prenotazione creata con " + lines.size() + " elemento/i, totale " + totalAmount + "€");

        // Checkout andato a buon fine -> rimuoviamo dal carrello solo gli
        // articoli appena acquistati (gli altri, se non selezionati, restano
        // per un checkout successivo). I loro hold sono già stati trasferiti
        // alle BookingLine sopra, quindi qui NON vanno rilasciati.
        cartService.removeCheckedOutItems(userId, itemsToCheckout.stream().map(CartItem::getId).toList());

        // NIENTE eventPublisher.publishBookingConfirmed() qui: quell'evento
        // manda esplicitamente "la tua prenotazione è stata confermata", ma a
        // questo punto lo stato è ancora PENDING (nessun pagamento è ancora
        // avvenuto) - va emesso una sola volta, in confirmPayment(), quando è
        // davvero vero. La notifica corretta per questo momento è solo quella
        // sotto ("in attesa"), che non promette nulla che non sia già successo.
        sendNotification(userId, "Prenotazione in attesa 🛒", "Hai creato una prenotazione di " + totalAmount + "€.");

        return toResponseDTO(savedBooking, userId);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponseDTO> getUserHistory(String userId, Pageable pageable) {
        Page<Booking> bookings = bookingRepository.findVisibleToUser(userId, pageable);
        return bookings.map(booking -> toResponseDTO(booking, userId));
    }

    // 3. Permette al Leader di invitare gli amici
    // updatedAt si aggiorna da solo grazie a @UpdateTimestamp, non serve più gestirlo qui.
    @Transactional
    public BookingResponseDTO inviteFriend(Long bookingId, String leaderId, String friendId) {
        // Cerca il viaggio -> se non esiste, 404 grazie a ResourceNotFoundException
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Prenotazione non trovata!"));

        // Controllo di sicurezza: solo chi ha pagato (il leader) può invitare -> 403 se fallisce
        if (!booking.getUserId().equals(leaderId)) {
            throw new AccessDeniedException("Accesso negato: solo il creatore del viaggio può invitare amici.");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingStateException("Non puoi invitare amici a una prenotazione annullata.");
        }

        if (friendId == null || friendId.isBlank()) {
            throw new IllegalArgumentException("L'id dell'amico da invitare è obbligatorio.");
        }
        if (friendId.equals(leaderId)) {
            throw new IllegalArgumentException("Non puoi invitare te stesso al viaggio.");
        }
        // friendId deve essere un identificativo utente (sub Keycloak), non una
        // stringa qualsiasi: senza questo controllo si può aggiungere come
        // "partecipante" un valore arbitrario, mai verificato con user-auth-service.
        try {
            java.util.UUID.fromString(friendId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("L'id dell'amico da invitare non è un identificativo utente valido.");
        }
        if (booking.getParticipantIds().contains(friendId)) {
            throw new InvalidBookingStateException("Questo utente è già stato invitato al viaggio.");
        }
        // Verifica di esistenza vera e propria (chiamata a user-auth-service,
        // quindi fatta per ultima tra i controlli su friendId): un formato UUID
        // valido non garantisce che corrisponda a un utente reale.
        try {
            userAuthClient.getUserSummary(friendId);
        } catch (FeignException ex) {
            if (ex.status() == 404) {
                throw new ResourceNotFoundException("L'utente da invitare non esiste.");
            }
            throw ex;
        }

        // Aggiunge l'amico alla lista e salva nel database
        booking.getParticipantIds().add(friendId);
        bookingRepository.save(booking);

        // Evento di audit: partecipante aggiunto. performedBy è il leader che ha
        // eseguito l'azione, non il friendId invitato (quello va nel dettaglio).
        auditService.log(booking, leaderId, AuditAction.PARTICIPANT_ADDED,
                "Invitato partecipante con id " + friendId);

        sendNotification(friendId, "Nuovo compagno di viaggio! ✈️", "Sei stato aggiunto a un viaggio.");

        // Restituisce il viaggio aggiornato (con isLeader = true, dato che l'ha chiamato il leader)
        return toResponseDTO(booking, leaderId);
    }

    // 4. Storico eventi di una prenotazione.
    // Autorizzazione: può vederlo solo il leader o uno dei partecipanti invitati.
    @Transactional(readOnly = true)
    public List<AuditLogEntryDTO> getAuditHistory(Long bookingId, String requesterId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Prenotazione non trovata!"));

        boolean isLeader = booking.getUserId().equals(requesterId);
        boolean isParticipant = booking.getParticipantIds().contains(requesterId);

        if (!isLeader && !isParticipant) {
            throw new AccessDeniedException("Accesso negato: non fai parte di questa prenotazione.");
        }

        return auditService.getHistory(bookingId).stream()
                .map(entry -> new AuditLogEntryDTO(
                        entry.getAction(),
                        entry.getPerformedBy(),
                        entry.getDetails(),
                        entry.getCreatedAt()))
                .collect(Collectors.toList());
    }

    // 5. Associa un passeggero (con documento congelato) a una riga di prenotazione.
    // I dati arrivano già risolti da Android (autocompilati leggendo da user-auth-service,
    // o inseriti a mano) - qui li salviamo così come sono, senza richiamare altri servizi.
    // Autorizzazione: solo il leader della Booking a cui appartiene la riga può farlo,
    // stesso criterio già usato per inviteFriend().
    @Transactional
    public void addPassenger(Long bookingLineId, String requesterId, PassengerRequestDTO request) {
        BookingLine line = bookingLineRepository.findById(bookingLineId)
                .orElseThrow(() -> new ResourceNotFoundException("Riga di prenotazione non trovata!"));

        Booking booking = line.getBooking();
        if (!booking.getUserId().equals(requesterId)) {
            throw new AccessDeniedException("Accesso negato: solo il creatore del viaggio può aggiungere passeggeri.");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingStateException("Non puoi aggiungere passeggeri a una prenotazione annullata.");
        }

        // quantity può essere null su righe create prima di questa modifica
        // (colonna aggiunta senza NOT NULL, vedi BookingLine): in quel caso
        // non abbiamo un limite noto, quindi non blocchiamo l'inserimento.
        if (line.getQuantity() != null && line.getPassengers().size() >= line.getQuantity()) {
            throw new InvalidBookingStateException(
                    "Questa riga di prenotazione ha già il numero massimo di passeggeri (" + line.getQuantity() + ").");
        }

        Passenger passenger = Passenger.builder()
                .bookingLine(line)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phoneNumber(request.phoneNumber())
                .taxCode(request.taxCode())
                .documentType(request.documentType())
                .documentNumber(request.documentNumber())
                .documentExpirationDate(request.documentExpirationDate())
                .issuingCountry(request.issuingCountry())
                .checkedIn(false)
                .build();

        passengerRepository.save(passenger);

        auditService.log(booking, requesterId, AuditAction.PASSENGER_ADDED,
                "Aggiunto passeggero " + request.firstName() + " " + request.lastName()
                        + " alla riga " + bookingLineId);
    }

    // 6. Conferma una prenotazione dopo un pagamento riuscito: verifica che il
    // chiamante sia il proprietario, che la Booking sia ancora in attesa e che
    // l'importo pagato corrisponda esattamente al totale, poi conferma
    // definitivamente ogni hold aperto su catalog-service e passa lo stato a CONFIRMED.
    //
    // NIENTE @Transactional sul metodo: le chiamate a catalog-service (fase 2
    // sotto) possono impiegare fino a response-timeout=15s, e tenerle dentro
    // una transazione terrebbe bloccata una connessione al nostro DB per tutto
    // quel tempo, esaurendo il pool sotto carico (vedi audit §2.9). Le uniche
    // due transazioni reali sono le fasi 1 e 3, entrambe brevi e locali.
    public Booking confirmPayment(Long bookingId, String userId, BigDecimal amount) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        // Fase 1 (transazione breve): valida SENZA ancora contattare catalog-service.
        // Se fallisce, nessuna chiamata remota viene mai fatta.
        tx.executeWithoutResult(status -> loadAndValidateForPayment(bookingId, userId, amount));

        // Fase 2 (FUORI da transazione): conferma ogni hold aperto su
        // catalog-service. Nota: gli hold già confermati con successo in questo
        // stesso ciclo NON possono più essere rilasciati da catalog-service (un
        // hold CONFIRMED rifiuta la release), quindi un fallimento a metà lista
        // può lasciare qualche riga "confermata" lato catalogo anche se la
        // Booking resta PENDING: è un limite del modello di hold di
        // catalog-service, non risolvibile da qui.
        //
        // Un 409 da catalog-service qui significa che il blocco non è più
        // confermabile (scaduto, quindi la camera/il posto può essere finito a
        // qualcun altro nel frattempo): capita soprattutto quando si riprova il
        // pagamento di una Booking rimasta PENDING a lungo. Lo traduciamo in un
        // messaggio chiaro invece di lasciarlo risalire come generico errore di
        // integrazione (vedi GlobalExceptionHandler.handleFeignException).
        Booking bookingWithLines = bookingRepository.findByIdWithLines(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Prenotazione non trovata!"));
        for (BookingLine line : bookingWithLines.getLines()) {
            if (line.getHoldId() != null) {
                try {
                    catalogClient.confirmHold(line.getHoldId());
                } catch (FeignException ex) {
                    if (ex.status() == 409) {
                        throw new InvalidBookingStateException(
                                "Uno o più articoli di questa prenotazione non sono più disponibili: il blocco temporaneo è scaduto, probabilmente qualcun altro li ha prenotati nel frattempo. Annulla la prenotazione e riprova dal catalogo.");
                    }
                    throw ex;
                }
            }
        }

        // Fase 3 (transazione breve): ricarica e ri-valida da capo (non solo lo
        // stato) prima di confermare davvero. Tra la fase 1 e qui sono passate
        // le chiamate HTTP di fase 2: il ricontrollo + @Version su Booking (B5)
        // proteggono dal caso in cui, in quella finestra, qualcun altro abbia
        // già annullato o pagato questa stessa prenotazione.
        return tx.execute(status -> {
            Booking booking = loadAndValidateForPayment(bookingId, userId, amount);

            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            auditService.log(booking, userId, AuditAction.STATUS_CHANGED,
                    "Pagamento di " + amount + "€ approvato, stato aggiornato a CONFIRMED");

            eventPublisher.publishBookingConfirmed(booking);

            sendNotification(userId, "Prenotazione Confermata! 🎉", "Il pagamento è andato a buon fine.");

            return booking;
        });
    }

    private Booking loadAndValidateForPayment(Long bookingId, String userId, BigDecimal amount) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Prenotazione non trovata!"));

        if (!booking.getUserId().equals(userId)) {
            throw new AccessDeniedException("Accesso negato: solo il creatore del viaggio può pagare questa prenotazione.");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidBookingStateException(
                    "La prenotazione non è in attesa di pagamento (stato attuale: " + booking.getStatus() + ").");
        }
        if (amount == null || amount.compareTo(booking.getTotalAmount()) != 0) {
            throw new PaymentValidationException(
                    "L'importo del pagamento non corrisponde al totale della prenotazione (" + booking.getTotalAmount() + "€).");
        }
        return booking;
    }

    // 7. Annulla una prenotazione: solo il leader può farlo. Se non era ancora
    // confermata, rilascia gli eventuali hold ancora aperti (erano HELD, non
    // CONFIRMED). Se era già CONFIRMED, avvia il rimborso tramite PaymentService
    // (gli hold restano confermati lato catalog-service: vedi nota in confirmPayment).
    @Transactional
    public BookingResponseDTO cancelBooking(Long bookingId, String requesterId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Prenotazione non trovata!"));

        if (!booking.getUserId().equals(requesterId)) {
            throw new AccessDeniedException("Accesso negato: solo il creatore del viaggio può annullare la prenotazione.");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingStateException("La prenotazione è già annullata.");
        }

        boolean wasConfirmed = booking.getStatus() == BookingStatus.CONFIRMED;

        if (!wasConfirmed) {
            for (BookingLine line : booking.getLines()) {
                if (line.getHoldId() != null) {
                    try {
                        catalogClient.releaseHold(line.getHoldId());
                    } catch (RuntimeException ex) {
                        // Un hold già scaduto/rilasciato da solo lato catalog-service (es.
                        // dopo 15 minuti) non deve impedire l'annullamento della prenotazione:
                        // logghiamo e proseguiamo, stesso criterio di ShoppingCartService.
                        log.warn("Impossibile rilasciare il blocco {} durante l'annullamento della prenotazione {}: {}",
                                line.getHoldId(), bookingId, ex.getMessage());
                    }
                }
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        if (wasConfirmed) {
            paymentService.refund(bookingId, booking.getTotalAmount());
        }

        auditService.log(booking, requesterId, AuditAction.STATUS_CHANGED,
                "Prenotazione annullata" + (wasConfirmed ? " e rimborso avviato" : ""));


        return toResponseDTO(booking, requesterId);
    }

    // 8. Prenotazioni fatte da ALTRI utenti sugli annunci pubblicati da chi chiama:
    // prima chiediamo a catalog-service quali item sono suoi (getMyItems), poi
    // cerchiamo tra tutte le Booking quelle con almeno una riga su quegli item.
    // Filtriamo di nuovo per catalogItemId dentro il ciclo perché una Booking può
    // contenere anche righe su annunci di ALTRI host, che non vanno restituite.
    @Transactional(readOnly = true)
    public List<ReceivedBookingLineDTO> getReceivedBookings() {
        List<Long> myItemIds = catalogClient.getMyItems().stream()
                .map(CatalogItemSummaryDTO::id)
                .toList();

        if (myItemIds.isEmpty()) {
            return List.of();
        }

        List<ReceivedBookingLineDTO> result = new ArrayList<>();
        for (Booking booking : bookingRepository.findDistinctByLines_CatalogItemIdIn(myItemIds)) {
            for (BookingLine line : booking.getLines()) {
                if (myItemIds.contains(line.getCatalogItemId())) {
                    result.add(new ReceivedBookingLineDTO(
                            booking.getId(),
                            booking.getUserId(),
                            line.getCatalogItemId(),
                            line.getQuantity(),
                            line.getPrice(),
                            line.getCheckIn(),
                            line.getCheckOut(),
                            booking.getStatus(),
                            booking.getBookingDate()));
                }
            }
        }
        return result;
    }

    // Mappa una Booking nel suo DTO pubblico, calcolando isLeader rispetto a chi
    // sta guardando (viewerId): lo stesso viaggio appare con isLeader diverso
    // a seconda che lo richieda il leader o uno dei partecipanti invitati.
    private BookingResponseDTO toResponseDTO(Booking booking, String viewerId) {
        boolean isLeader = booking.getUserId().equals(viewerId);

        List<BookingLineDTO> lines = booking.getLines().stream()
                .map(line -> new BookingLineDTO(
                        line.getId(),
                        line.getCatalogItemId(),
                        line.getPrice(),
                        line.getQuantity(),
                        line.getRoomTypeId(),
                        line.getFareClassId(),
                        line.getCheckIn(),
                        line.getCheckOut(),
                        line.getPassengers().size()))
                .collect(Collectors.toList());

        return new BookingResponseDTO(
                booking.getId(),
                booking.getTotalAmount(),
                booking.getBookingDate(),
                booking.getStatus(),
                isLeader,
                new ArrayList<>(booking.getParticipantIds()),
                lines);
    }

    // 9. Passeggeri (con il relativo QR di check-in, se già generato) di una
    // riga di prenotazione. Autorizzazione: leader o uno dei partecipanti del
    // viaggio, stesso criterio di getAuditHistory - chiunque abbia il telefono
    // in mano deve poter mostrare il "biglietto", non solo chi ha pagato.
    @Transactional(readOnly = true)
    public List<PassengerResponseDTO> getPassengersForLine(Long bookingLineId, String requesterId) {
        BookingLine line = bookingLineRepository.findById(bookingLineId)
                .orElseThrow(() -> new ResourceNotFoundException("Riga di prenotazione non trovata!"));

        Booking booking = line.getBooking();
        boolean isLeader = booking.getUserId().equals(requesterId);
        boolean isParticipant = booking.getParticipantIds().contains(requesterId);
        if (!isLeader && !isParticipant) {
            throw new AccessDeniedException("Accesso negato: non fai parte di questa prenotazione.");
        }

        // Codice fiscale e numero documento sono dati sensibili di terzi: solo il
        // leader (che li ha inseriti) li vede in chiaro. Gli altri partecipanti
        // vedono comunque la lista viaggiatori (serve per sapere chi c'è), ma con
        // questi due campi mascherati.
        return line.getPassengers().stream()
                .map(passenger -> new PassengerResponseDTO(
                        passenger.getId(),
                        passenger.getFirstName(),
                        passenger.getLastName(),
                        passenger.getPhoneNumber(),
                        maskUnlessLeader(passenger.getTaxCode(), isLeader),
                        passenger.getDocumentType(),
                        maskUnlessLeader(passenger.getDocumentNumber(), isLeader),
                        passenger.getQrCodeData(),
                        passenger.isCheckedIn()))
                .collect(Collectors.toList());
    }

    private static final int MASKED_VISIBLE_SUFFIX_LENGTH = 4;

    // Mostra solo le ultime 4 cifre/caratteri, mascherando il resto con "*"
    // (stesso criterio suggerito per altra PII simile nel progetto, es. il
    // numero di documento in user-auth-service).
    private String maskUnlessLeader(String value, boolean isLeader) {
        if (isLeader || value == null || value.isBlank()) {
            return value;
        }
        if (value.length() <= MASKED_VISIBLE_SUFFIX_LENGTH) {
            return "*".repeat(value.length());
        }
        return "*".repeat(value.length() - MASKED_VISIBLE_SUFFIX_LENGTH) + value.substring(value.length() - MASKED_VISIBLE_SUFFIX_LENGTH);
    }

    public boolean hasUserBookedItem(String userId, Long catalogItemId) {
        return bookingRepository.existsByUserIdAndLines_CatalogItemIdAndStatus(
                userId, catalogItemId, BookingStatus.CONFIRMED);
    }


    private void sendNotification(String userId, String title, String message) {
        try {
            BookingNotificationEvent event = new BookingNotificationEvent(userId, title, message);
            rabbitTemplate.convertAndSend("notification_queue", event);
        } catch (Exception e) {
            // Ignora o logga l'errore
        }
    }



}
