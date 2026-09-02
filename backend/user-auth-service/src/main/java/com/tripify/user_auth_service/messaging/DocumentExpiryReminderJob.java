package com.tripify.user_auth_service.messaging;

import com.tripify.user_auth_service.config.RabbitMQConfig;
import com.tripify.user_auth_service.entity.TravelDocument;
import com.tripify.user_auth_service.repository.TravelDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentExpiryReminderJob {

    private static final List<Integer> REMINDER_DAYS = List.of(30, 7, 1);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TravelDocumentRepository documentRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(cron = "${document-expiry.reminder-cron:0 0 9 * * *}")
    public void sendExpiryReminders() {
        LocalDate today = LocalDate.now();
        List<LocalDate> targetDates = REMINDER_DAYS.stream().map(today::plusDays).toList();

        List<TravelDocument> expiring = documentRepository.findByExpirationDateIn(targetDates);
        if (expiring.isEmpty()) {
            return;
        }

        int sent = 0;
        for (TravelDocument doc : expiring) {
            String userId = doc.getUser() != null ? doc.getUser().getUsername() : null;
            if (userId == null || userId.isBlank()) {
                log.warn("Documento {} in scadenza ma l'utente non ha un subject Keycloak sincronizzato, promemoria saltato", doc.getId());
                continue;
            }
            long daysLeft = ChronoUnit.DAYS.between(today, doc.getExpirationDate());
            NotificationEvent event = new NotificationEvent(
                    userId,
                    "Documento in scadenza",
                    "Il tuo documento (" + humanType(doc.getDocumentType()) + ") scade il "
                            + doc.getExpirationDate().format(DATE_FORMAT)
                            + " (tra " + daysLeft + (daysLeft == 1 ? " giorno" : " giorni") + ")."
            );
            try {
                rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_QUEUE, event);
                sent++;
            } catch (Exception e) {
                log.warn("Invio promemoria scadenza per il documento {} non riuscito: {}", doc.getId(), e.getMessage());
            }
        }
        log.info("Promemoria scadenza documenti inviati: {}", sent);
    }

    private String humanType(String type) {
        if (type == null || type.isBlank()) {
            return "documento";
        }
        String normalized = type.toUpperCase();
        if (normalized.contains("PASSAP") || normalized.contains("PASSPORT")) {
            return "passaporto";
        }
        if (normalized.contains("IDENTIT") || normalized.contains("ID_CARD") || normalized.contains("ID CARD")) {
            return "carta d'identità";
        }
        if (normalized.contains("PATENTE") || normalized.contains("DRIVING") || normalized.contains("LICEN")) {
            return "patente";
        }
        return type.toLowerCase();
    }
}
