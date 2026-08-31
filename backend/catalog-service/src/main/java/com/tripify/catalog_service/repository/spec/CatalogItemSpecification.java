package com.tripify.catalog_service.repository.spec;

import com.tripify.catalog_service.entity.Activity;
import com.tripify.catalog_service.entity.CatalogItem;
import com.tripify.catalog_service.entity.FareClass;
import com.tripify.catalog_service.entity.Flight;
import com.tripify.catalog_service.entity.Hotel;
import com.tripify.catalog_service.entity.RoomType;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CatalogItemSpecification {

    public static Specification<CatalogItem> withDynamicFilters(
            String category,
            String keyword,
            BigDecimal maxPrice,
            Integer minRating,
            String destination,
            String departure,
            Boolean guideIncluded,
            List<String> amenities,
            Boolean directOnly,
            LocalDate departureDate,
            Integer minSeats
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            // 0. Solo annunci attivi: il fetch diretto per id resta possibile anche per
            // un item disattivato (es. per chi lo ha già prenotato), ma non deve comparire
            // in ricerca.
            predicates.add(cb.isTrue(root.get("isActive")));

            // 1. Categoria
            if (category != null && !category.equalsIgnoreCase("Tutti")) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
            }

            // 2. Keyword libera (titolo OR descrizione)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String likePattern = "%" + keyword.toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), likePattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), likePattern);
                predicates.add(cb.or(titleMatch, descMatch));
            }

            // 3. Destinazione: arrivalCity per i voli, city per hotel/activity)
            if (destination != null && !destination.trim().isEmpty()) {
                String destPattern = "%" + destination.trim().toLowerCase() + "%";

                Root<Flight> flightRoot = cb.treat(root, Flight.class);
                Root<Hotel> hotelRoot = cb.treat(root, Hotel.class);
                Root<Activity> activityRoot = cb.treat(root, Activity.class);

                Predicate flightMatch = cb.and(
                        cb.equal(root.type(), Flight.class),
                        cb.like(cb.lower(flightRoot.get("arrivalCity")), destPattern)
                );
                Predicate hotelMatch = cb.and(
                        cb.equal(root.type(), Hotel.class),
                        cb.like(cb.lower(hotelRoot.get("city")), destPattern)
                );
                Predicate activityMatch = cb.and(
                        cb.equal(root.type(), Activity.class),
                        cb.like(cb.lower(activityRoot.get("city")), destPattern)
                );

                predicates.add(cb.or(flightMatch, hotelMatch, activityMatch));
            }

            // 4. Partenza: solo per i voli, ora sulla città invece del codice IATA
            if (departure != null && !departure.trim().isEmpty()) {
                Root<Flight> flightRoot = cb.treat(root, Flight.class);
                Predicate isFlight = cb.equal(root.type(), Flight.class);
                Predicate departureMatch = cb.like(cb.lower(flightRoot.get("departureCity")), "%" + departure.trim().toLowerCase() + "%");
                predicates.add(cb.or(cb.not(isFlight), departureMatch));
            }

            // 5. Prezzo massimo: confronta la tariffa/camera più economica, non il prezzo
            // base dell'item, che è lo stesso prezzo mostrato in ricerca (vedi
            // CatalogMapper.cheapestPrice) — altrimenti un hotel con una camera economica
            // ma un prezzo di listino alto verrebbe escluso ingiustamente.
            if (maxPrice != null) {
                Root<Flight> flightRoot = cb.treat(root, Flight.class);
                Root<Hotel> hotelRoot = cb.treat(root, Hotel.class);
                Predicate isFlight = cb.equal(root.type(), Flight.class);
                Predicate isHotel = cb.equal(root.type(), Hotel.class);

                Subquery<BigDecimal> minFare = query.subquery(BigDecimal.class);
                Root<Flight> fareCorrelated = minFare.correlate(flightRoot);
                Join<Flight, FareClass> fareJoin = fareCorrelated.join("fareClasses");
                minFare.select(cb.min(fareJoin.get("price")));

                Subquery<BigDecimal> minRoom = query.subquery(BigDecimal.class);
                Root<Hotel> roomCorrelated = minRoom.correlate(hotelRoot);
                Join<Hotel, RoomType> roomJoin = roomCorrelated.join("roomTypes");
                minRoom.select(cb.min(roomJoin.get("price")));

                // Se non ci sono tariffe/camere (caso limite), ricadiamo sul prezzo base,
                // stesso fallback usato da CatalogMapper.cheapestPrice().
                Predicate flightOk = cb.and(isFlight, cb.lessThanOrEqualTo(cb.coalesce(minFare, flightRoot.get("price")), maxPrice));
                Predicate hotelOk = cb.and(isHotel, cb.lessThanOrEqualTo(cb.coalesce(minRoom, hotelRoot.get("price")), maxPrice));
                Predicate otherOk = cb.and(cb.not(isFlight), cb.not(isHotel), cb.lessThanOrEqualTo(root.get("price"), maxPrice));

                predicates.add(cb.or(flightOk, hotelOk, otherOk));
            }

            // 6. Rating minimo
            if (minRating != null && minRating > 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating));
            }

            // 7. Solo voli diretti
            if (Boolean.TRUE.equals(directOnly)) {
                Root<Flight> flightRoot = cb.treat(root, Flight.class);
                Predicate isFlight = cb.equal(root.type(), Flight.class);
                Predicate isDirect = cb.equal(flightRoot.get("stops"), 0);
                predicates.add(cb.or(cb.not(isFlight), isDirect));
            }

            // 8. Guida inclusa
            if (Boolean.TRUE.equals(guideIncluded)) {
                Root<Activity> activityRoot = cb.treat(root, Activity.class);
                Predicate isActivity = cb.equal(root.type(), Activity.class);
                Predicate hasGuide = cb.isTrue(activityRoot.get("guideIncluded"));
                predicates.add(cb.or(cb.not(isActivity), hasGuide));
            }

            // 9b. Data di partenza: solo per i voli, confronta l'intera giornata locale
            if (departureDate != null) {
                Root<Flight> flightRoot = cb.treat(root, Flight.class);
                Predicate isFlight = cb.equal(root.type(), Flight.class);
                Predicate onThatDay = cb.between(
                        flightRoot.get("departureTime"),
                        departureDate.atStartOfDay(),
                        departureDate.plusDays(1).atStartOfDay().minusNanos(1)
                );
                predicates.add(cb.or(cb.not(isFlight), onThatDay));
            }

            // 9c. Posti minimi disponibili: solo per i voli
            if (minSeats != null && minSeats > 0) {
                Root<Flight> flightRoot = cb.treat(root, Flight.class);
                Predicate isFlight = cb.equal(root.type(), Flight.class);
                Predicate hasEnoughSeats = cb.greaterThanOrEqualTo(flightRoot.get("totalSeats"), minSeats);
                predicates.add(cb.or(cb.not(isFlight), hasEnoughSeats));
            }

            // 9. Amenities (tutte richieste)
            if (amenities != null && !amenities.isEmpty()) {
                Root<Hotel> hotelRoot = cb.treat(root, Hotel.class);
                Predicate isHotel = cb.equal(root.type(), Hotel.class);

                List<Predicate> amenityChecks = new ArrayList<>();
                for (String amenity : amenities) {
                    Subquery<Long> subquery = query.subquery(Long.class);
                    Root<Hotel> subRoot = subquery.correlate(hotelRoot);
                    Join<Hotel, String> amenityJoin = subRoot.join("amenities");
                    subquery.select(cb.literal(1L))
                            .where(cb.equal(cb.lower(amenityJoin), amenity.toLowerCase()));
                    amenityChecks.add(cb.exists(subquery));
                }
                Predicate hasAllAmenities = cb.and(amenityChecks.toArray(new Predicate[0]));
                predicates.add(cb.or(cb.not(isHotel), hasAllAmenities));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}