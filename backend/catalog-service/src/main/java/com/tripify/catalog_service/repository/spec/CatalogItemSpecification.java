package com.tripify.catalog_service.repository.spec;

import com.tripify.catalog_service.entity.Activity;
import com.tripify.catalog_service.entity.CatalogItem;
import com.tripify.catalog_service.entity.Flight;
import com.tripify.catalog_service.entity.Hotel;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
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
            Boolean directOnly
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

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

            // 5. Prezzo massimo
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
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