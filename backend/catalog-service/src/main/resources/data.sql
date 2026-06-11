-- Pulizia iniziale (opzionale ma sicura)
-- Rispettare l'ordine per evitare errori di Foreign Key (cancellare prima i figli, poi i padri)
DELETE FROM itinerary_items;
DELETE FROM itineraries;
DELETE FROM catalog_images; -- NUOVA TABELLA IMMAGINI
DELETE FROM flight_details;
DELETE FROM hotel_details;
DELETE FROM activity_details;
DELETE FROM catalog_items;

-- ==========================================
-- 1. INSERIMENTO VOLO (ID 1)
-- ==========================================
INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating)
VALUES (1, 'Volo Roma - New York', 'Volo diretto operato da ITA Airways', 550.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);

INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_time, arrival_time, available_seats)
VALUES (1, 'FCO', 'JFK', '2026-06-01 10:00:00', '2026-06-01 14:00:00', 120);

-- Aggiunta di 2 foto per il volo (collegate all'ID 1)
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1436491865332-7a61a109cc05?q=80&w=1000&auto=format&fit=crop', 1),
                                                            ('https://images.unsplash.com/photo-1542296332-2e4473faf563?q=80&w=1000&auto=format&fit=crop', 1);

-- ==========================================
-- 2. INSERIMENTO HOTEL (ID 2)
-- ==========================================
INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating)
VALUES (2, 'Hotel Hilton Times Square', 'Soggiorno di lusso nel cuore di Manhattan', 250.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 5);

INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms)
VALUES (2, 40.7589, -73.9851, 'Double Deluxe', 15);

-- Aggiunta di 3 foto per l'hotel (collegate all'ID 2)
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1566073771259-6a8506099945?q=80&w=1000&auto=format&fit=crop', 2),
                                                            ('https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?q=80&w=1000&auto=format&fit=crop', 2),
                                                            ('https://images.unsplash.com/photo-1582719478250-c89402bb6539?q=80&w=1000&auto=format&fit=crop', 2);

-- ==========================================
-- SINCRONIZZAZIONE SEQUENZE POSTGRESQL
-- ==========================================
-- Ripristiniamo i contatori per evitare l'errore "duplicate key value violates unique constraint" ai prossimi inserimenti via API
SELECT setval(pg_get_serial_sequence('catalog_items', 'id'), (SELECT MAX(id) FROM catalog_items));

-- Usa COALESCE per evitare errori se la tabella è vuota, forzando a 1 come fallback
SELECT setval(pg_get_serial_sequence('catalog_images', 'id'), COALESCE((SELECT MAX(id) FROM catalog_images), 1));