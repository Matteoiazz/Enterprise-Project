package com.tripify.catalog_service.repository.spec;

import com.tripify.catalog_service.entity.Activity;
import com.tripify.catalog_service.entity.CatalogItem;
import com.tripify.catalog_service.entity.Flight;
import com.tripify.catalog_service.entity.Hotel;
import com.tripify.catalog_service.entity.RoomType;
import com.tripify.catalog_service.repository.CatalogItemRepository;
import com.tripify.catalog_service.repository.RoomTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CatalogItemSpecificationTest {

    @Autowired
    private CatalogItemRepository repository;
    @Autowired
    private RoomTypeRepository roomTypeRepository;

    private final UUID hostId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // 1. Volo diretto Roma -> Milano
        Flight flightDiretto = new Flight();
        flightDiretto.setHostId(hostId);
        flightDiretto.setTitle("Volo diretto Roma Milano");
        flightDiretto.setDescription("Volo comodo");
        flightDiretto.setPrice(new BigDecimal("90"));
        flightDiretto.setCurrency("EUR");
        flightDiretto.setCategory("Voli");
        flightDiretto.setRating(4);
        flightDiretto.setDepartureAirport("FCO");
        flightDiretto.setArrivalAirport("LIN");
        flightDiretto.setDepartureCity("Roma");
        flightDiretto.setArrivalCity("Milano");
        flightDiretto.setDepartureTime(LocalDateTime.now());
        flightDiretto.setArrivalTime(LocalDateTime.now().plusHours(1));
        flightDiretto.setTotalSeats(50);
        flightDiretto.setStops(0);

        // 2. Volo con scalo Roma -> Londra
        Flight flightConScalo = new Flight();
        flightConScalo.setHostId(hostId);
        flightConScalo.setTitle("Volo con scalo Roma Londra");
        flightConScalo.setDescription("Volo economico");
        flightConScalo.setPrice(new BigDecimal("60"));
        flightConScalo.setCurrency("EUR");
        flightConScalo.setCategory("Voli");
        flightConScalo.setRating(3);
        flightConScalo.setDepartureAirport("FCO");
        flightConScalo.setArrivalAirport("LHR");
        flightConScalo.setDepartureCity("Roma");
        flightConScalo.setArrivalCity("Londra");
        flightConScalo.setDepartureTime(LocalDateTime.now());
        flightConScalo.setArrivalTime(LocalDateTime.now().plusHours(3));
        flightConScalo.setTotalSeats(20);
        flightConScalo.setStops(1);

        // 3. Hotel con Wi-Fi e Piscina, rating alto
        Hotel hotelLusso = new Hotel();
        hotelLusso.setHostId(hostId);
        hotelLusso.setTitle("Hotel di lusso a Roma");
        hotelLusso.setDescription("Hotel 5 stelle");
        hotelLusso.setPrice(new BigDecimal("300"));
        hotelLusso.setCurrency("EUR");
        hotelLusso.setCategory("Hotel");
        hotelLusso.setRating(5);
        hotelLusso.setLocationLat(41.9);
        hotelLusso.setLocationLng(12.5);
        hotelLusso.setAddress("Via Roma 1");
        hotelLusso.setCity("Roma");
        hotelLusso.setAmenities(List.of("Wi-Fi", "Piscina"));

        // 4. Hotel economico senza amenities, rating basso
        Hotel hotelEconomico = new Hotel();
        hotelEconomico.setHostId(hostId);
        hotelEconomico.setTitle("Hotel economico a Milano");
        hotelEconomico.setDescription("Hotel semplice");
        hotelEconomico.setPrice(new BigDecimal("50"));
        hotelEconomico.setCurrency("EUR");
        hotelEconomico.setCategory("Hotel");
        hotelEconomico.setRating(2);
        hotelEconomico.setLocationLat(45.4);
        hotelEconomico.setLocationLng(9.1);
        hotelEconomico.setAddress("Via Milano 1");
        hotelEconomico.setCity("Milano");
        hotelEconomico.setAmenities(List.of());

        // 5. Attività con guida inclusa
        Activity attivitaConGuida = new Activity();
        attivitaConGuida.setHostId(hostId);
        attivitaConGuida.setTitle("Trekking guidato");
        attivitaConGuida.setDescription("Escursione in montagna");
        attivitaConGuida.setPrice(new BigDecimal("40"));
        attivitaConGuida.setCurrency("EUR");
        attivitaConGuida.setCategory("Attività");
        attivitaConGuida.setRating(4);
        attivitaConGuida.setActivityType("Sport");
        attivitaConGuida.setDuration("4 ore");
        attivitaConGuida.setCity("Roma");
        attivitaConGuida.setGuideIncluded(true);

        // 6. Attività senza guida
        Activity attivitaLibera = new Activity();
        attivitaLibera.setHostId(hostId);
        attivitaLibera.setTitle("Passeggiata libera");
        attivitaLibera.setDescription("Esplorazione autonoma");
        attivitaLibera.setPrice(new BigDecimal("15"));
        attivitaLibera.setCurrency("EUR");
        attivitaLibera.setCategory("Attività");
        attivitaLibera.setRating(3);
        attivitaLibera.setActivityType("Cultura");
        attivitaLibera.setDuration("2 ore");
        attivitaLibera.setCity("Milano");
        attivitaLibera.setGuideIncluded(false);

        repository.saveAll(List.of(
                flightDiretto, flightConScalo, hotelLusso, hotelEconomico, attivitaConGuida, attivitaLibera
        ));
    }

    @Test
    void filtroCategoria_restituisceSoloIVoli() {
        var spec = CatalogItemSpecification.withDynamicFilters(
                "Voli", null, null, null, null, null, null, null, null, null, null
        );
        List<CatalogItem> risultati = repository.findAll(spec);

        assertThat(risultati).hasSize(2);
        assertThat(risultati).allMatch(item -> item instanceof Flight);
    }

    @Test
    void filtroCategoriaTutti_nonEsclude() {
        var spec = CatalogItemSpecification.withDynamicFilters(
                "Tutti", null, null, null, null, null, null, null, null, null, null
        );
        List<CatalogItem> risultati = repository.findAll(spec);

        assertThat(risultati).hasSize(6);
    }

    @Test
    void filtroPrezzoMassimo_escludegliItemPiuCari() {
        var spec = CatalogItemSpecification.withDynamicFilters(
                "Tutti", null, new BigDecimal("60"), null, null, null, null, null, null, null, null
        );
        List<CatalogItem> risultati = repository.findAll(spec);

        // Sotto o uguale a 60: flightConScalo(60), hotelEconomico(50), attivitaConGuida(40), attivitaLibera(15)
        assertThat(risultati).hasSize(4);
        assertThat(risultati).noneMatch(item -> item.getTitle().contains("lusso"));
    }

    @Test
    void filtroRatingMinimo_restituisceSoloItemConRatingAlto() {
        var spec = CatalogItemSpecification.withDynamicFilters(
                "Tutti", null, null, 4, null, null, null, null, null, null, null
        );
        List<CatalogItem> risultati = repository.findAll(spec);

        // Rating >= 4: flightDiretto(4), hotelLusso(5), attivitaConGuida(4)
        assertThat(risultati).hasSize(3);
    }

    @Test
    void filtroDestinazione_trovaVoliPerCittaDiArrivo() {
        var spec = CatalogItemSpecification.withDynamicFilters(
                "Tutti", null, null, null, "Londra", null, null, null, null, null, null
        );
        List<CatalogItem> risultati = repository.findAll(spec);

        assertThat(risultati).hasSize(1);
        assertThat(risultati.get(0).getTitle()).contains("Londra");
    }

    @Test
    void filtroDestinazione_trovaHotelEAttivitaPerCitta() {
        var spec = CatalogItemSpecification.withDynamicFilters(
                "Tutti", null, null, null, "Roma", null, null, null, null, null, null
        );
        List<CatalogItem> risultati = repository.findAll(spec);

        // A Roma: hotelLusso (city=Roma) e attivitaConGuida (city=Roma).
        // Il volo diretto ha arrivalCity=Milano, quindi NON deve comparire qui.
        assertThat(risultati).hasSize(2);
    }

    @Test
    void filtroSoloVoliDiretti_nonEscludeGliAltriTipi() {
        var spec = CatalogItemSpecification.withDynamicFilters(
                "Tutti", null, null, null, null, null, null, null, true, null, null
        );
        List<CatalogItem> risultati = repository.findAll(spec);

        // Deve escludere SOLO il volo con scalo, non toccare hotel/attività
        assertThat(risultati).hasSize(5);
        assertThat(risultati).noneMatch(item -> item instanceof Flight f && f.getStops() > 0);
    }

    @Test
    void filtroGuidaInclusa_nonEscludeGliAltriTipi() {
        var spec = CatalogItemSpecification.withDynamicFilters(
                "Tutti", null, null, null, null, null, true, null, null, null, null
        );
        List<CatalogItem> risultati = repository.findAll(spec);

        assertThat(risultati).hasSize(5);
        assertThat(risultati).noneMatch(item -> item instanceof Activity a && !a.isGuideIncluded());
    }

    @Test
    void filtroAmenities_richiedeTutteQuelleSelezionate() {
        var spec = CatalogItemSpecification.withDynamicFilters(
                "Tutti", null, null, null, null, null, null, List.of("Wi-Fi", "Piscina"), null, null, null
        );
        List<CatalogItem> risultati = repository.findAll(spec);

        assertThat(risultati).hasSize(5); // esclude solo hotelEconomico
        assertThat(risultati).noneMatch(item -> item.getTitle().contains("economico"));
    }

    @Test
    void filtroCombinato_categoriaHotelEBudgetBasso() {
        var spec = CatalogItemSpecification.withDynamicFilters(
                "Hotel", null, new BigDecimal("100"), null, null, null, null, null, null, null, null
        );
        List<CatalogItem> risultati = repository.findAll(spec);

        assertThat(risultati).hasSize(1);
        assertThat(risultati.get(0).getTitle()).contains("economico");
    }

    @Test
    void filtroDataPartenza_trovaSoloIVoliDiQuelGiorno_nonEscludeGliAltriTipi() {
        var domani = java.time.LocalDate.now().plusDays(1);
        var spec = CatalogItemSpecification.withDynamicFilters(
                "Tutti", null, null, null, null, null, null, null, null, domani, null
        );
        List<CatalogItem> risultati = repository.findAll(spec);

        // Nessun volo del dataset parte "domani": devono restituirsi solo gli item non-volo.
        assertThat(risultati).hasSize(4);
        assertThat(risultati).noneMatch(item -> item instanceof Flight);
    }

    @Test
    void filtroPostiMinimi_escludeIVoliConPochiPosti() {
        var spec = CatalogItemSpecification.withDynamicFilters(
                "Tutti", null, null, null, null, null, null, null, null, null, 30
        );
        List<CatalogItem> risultati = repository.findAll(spec);

        // flightConScalo ha 20 posti, sotto soglia: deve sparire. flightDiretto(50) resta.
        assertThat(risultati).hasSize(5);
        assertThat(risultati).noneMatch(item -> item instanceof Flight f && f.getTotalSeats() < 30);
    }

    @Test
    void hasRoomTypeWithCapacity_includeSoloGliHotelConAlmenoUnaRoomTypeAbbastanzaGrande() {
        Hotel hotelGrande = new Hotel();
        hotelGrande.setHostId(hostId);
        hotelGrande.setTitle("Hotel con suite");
        hotelGrande.setPrice(new BigDecimal("200"));
        hotelGrande.setCurrency("EUR");
        hotelGrande.setCategory("Hotel");
        hotelGrande.setCity("Roma");
        hotelGrande.setLocationLat(41.9);
        hotelGrande.setLocationLng(12.5);
        hotelGrande.setAddress("Via Roma 10");
        hotelGrande = (Hotel) repository.save(hotelGrande);
        roomTypeRepository.save(RoomType.builder().hotel(hotelGrande).name("Suite").price(new BigDecimal("200")).totalRooms(5).build());

        Hotel hotelPiccolo = new Hotel();
        hotelPiccolo.setHostId(hostId);
        hotelPiccolo.setTitle("Hotel con singola");
        hotelPiccolo.setPrice(new BigDecimal("80"));
        hotelPiccolo.setCurrency("EUR");
        hotelPiccolo.setCategory("Hotel");
        hotelPiccolo.setCity("Roma");
        hotelPiccolo.setLocationLat(41.9);
        hotelPiccolo.setLocationLng(12.5);
        hotelPiccolo.setAddress("Via Roma 11");
        hotelPiccolo = (Hotel) repository.save(hotelPiccolo);
        roomTypeRepository.save(RoomType.builder().hotel(hotelPiccolo).name("Singola").price(new BigDecimal("80")).totalRooms(1).build());

        var spec = CatalogItemSpecification.withDynamicFilters("Tutti", null, null, null, null, null, null, null, null, null, null)
                .and(CatalogItemSpecification.hasRoomTypeWithCapacity(3));
        List<CatalogItem> risultati = repository.findAll(spec);

        assertThat(risultati).extracting(CatalogItem::getTitle).contains("Hotel con suite");
        assertThat(risultati).extracting(CatalogItem::getTitle).doesNotContain("Hotel con singola");
        // Il filtro riguarda solo gli hotel: voli/attività del fixture condiviso non vengono toccati.
        assertThat(risultati).anyMatch(item -> !(item instanceof Hotel));
    }
}