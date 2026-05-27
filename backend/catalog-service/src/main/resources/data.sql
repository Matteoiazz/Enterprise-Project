-- Pulizia iniziale (opzionale ma sicura)
DELETE FROM itinerary_items;
DELETE FROM itineraries;
DELETE FROM flight_details;
DELETE FROM hotel_details;
DELETE FROM activity_details;
DELETE FROM catalog_items;

-- 1. Inserimento Volo (ID 1)
INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active)
VALUES (1, 'Volo Roma - New York', 'Volo diretto operato da ITA Airways', 550.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true);

INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_time, arrival_time, available_seats)
VALUES (1, 'FCO', 'JFK', '2026-06-01 10:00:00', '2026-06-01 14:00:00', 120);

-- 2. Inserimento Hotel (ID 2)
INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active)
VALUES (2, 'Hotel Hilton Times Square', 'Soggiorno di lusso nel cuore di Manhattan', 250.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true);

INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms)
VALUES (2, 40.7589, -73.9851, 'Double Deluxe', 15);

-- Sincronizzazione dei generatori di ID (per evitare errori nei primi inserimenti manuali)
SELECT setval(pg_get_serial_sequence('catalog_items', 'id'), (SELECT MAX(id) FROM catalog_items));