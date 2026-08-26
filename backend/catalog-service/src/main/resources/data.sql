DELETE FROM room_holds;
DELETE FROM seat_holds;
DELETE FROM room_type_images;
DELETE FROM room_type_benefits;
DELETE FROM room_types;
DELETE FROM fare_classes;
DELETE FROM catalog_images;
DELETE FROM hotel_amenities;
DELETE FROM flight_details;
DELETE FROM hotel_details;
DELETE FROM activity_details;
DELETE FROM catalog_items;

-- ==========================================
-- VOLI (id 1-12)
-- Ogni volo ha capacita' totale (total_seats) e due classi tariffarie (fare_classes)
-- che si dividono quei posti con prezzi propri: la disponibilita' vera si calcola
-- sui SeatHold della classe scelta, non su questo numero.
-- ==========================================

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (1, 'Volo Roma - Milano', 'Volo di linea Roma Fiumicino - Milano Linate, ideale per viaggi di lavoro.', 89.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (1, 'FCO', 'LIN', 'Roma', 'Milano', CURRENT_DATE + 5 + TIME '07:30:00', CURRENT_DATE + 5 + TIME '08:45:00', 60, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (1, 1, 'Economy', 89.00, 48), (2, 1, 'Business', 205.00, 12);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1436491865332-7a61a109cc05?q=80&w=1000&auto=format&fit=crop', 1),
                                                            ('https://images.unsplash.com/photo-1569154941061-e231b4732ef1?q=80&w=1000&auto=format&fit=crop', 1);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (2, 'Volo Milano - Napoli', 'Collegamento diretto tra Malpensa e Capodichino, comodo per il weekend.', 75.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (2, 'MXP', 'NAP', 'Milano', 'Napoli', CURRENT_DATE + 8 + TIME '09:15:00', CURRENT_DATE + 8 + TIME '10:45:00', 40, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (3, 2, 'Economy', 75.00, 32), (4, 2, 'Business', 175.00, 8);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1502920917128-1aa500764cbd?q=80&w=1000&auto=format&fit=crop', 2),
                                                            ('https://images.unsplash.com/photo-1533904828757-e8f28a41d9db?q=80&w=1000&auto=format&fit=crop', 2);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (3, 'Volo Roma - Palermo', 'Volo diretto per la Sicilia, perfetto per esplorare Palermo e dintorni.', 68.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (3, 'FCO', 'PMO', 'Roma', 'Palermo', CURRENT_DATE + 3 + TIME '12:00:00', CURRENT_DATE + 3 + TIME '13:20:00', 55, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (5, 3, 'Economy', 68.00, 44), (6, 3, 'Business', 160.00, 11);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1523592121529-f6dde35f079e?q=80&w=1000&auto=format&fit=crop', 3),
                                                            ('https://images.unsplash.com/photo-1533105079780-92b9be482077?q=80&w=1000&auto=format&fit=crop', 3);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (4, 'Volo Torino - Catania', 'Volo con uno scalo, tariffa economica per raggiungere la Sicilia orientale.', 92.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 3);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (4, 'TRN', 'CTA', 'Torino', 'Catania', CURRENT_DATE + 12 + TIME '06:45:00', CURRENT_DATE + 12 + TIME '10:30:00', 35, 1);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (7, 4, 'Economy', 92.00, 28), (8, 4, 'Business', 210.00, 7);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1523731407965-2430cd12f5e4?q=80&w=1000&auto=format&fit=crop', 4),
                                                            ('https://images.unsplash.com/photo-1591604129939-f1efa4d9f7fa?q=80&w=1000&auto=format&fit=crop', 4);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (5, 'Volo Venezia - Bari', 'Volo diretto dal Nord-Est alla Puglia, ottimo per il mare adriatico.', 79.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (5, 'VCE', 'BRI', 'Venezia', 'Bari', CURRENT_DATE + 6 + TIME '15:20:00', CURRENT_DATE + 6 + TIME '16:50:00', 48, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (9, 5, 'Economy', 79.00, 38), (10, 5, 'Business', 180.00, 10);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1523906834658-6e24ef2386f9?q=80&w=1000&auto=format&fit=crop', 5),
                                                            ('https://images.unsplash.com/photo-1516483638261-f4dbaf036963?q=80&w=1000&auto=format&fit=crop', 5);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (6, 'Volo Bologna - Cagliari', 'Volo diretto per la Sardegna, ideale per le vacanze estive.', 85.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (6, 'BLQ', 'CAG', 'Bologna', 'Cagliari', CURRENT_DATE + 15 + TIME '11:10:00', CURRENT_DATE + 15 + TIME '12:35:00', 50, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (11, 6, 'Economy', 85.00, 40), (12, 6, 'Business', 195.00, 10);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1531572753322-ad063cecc140?q=80&w=1000&auto=format&fit=crop', 6),
                                                            ('https://images.unsplash.com/photo-1519046904884-53103b34b206?q=80&w=1000&auto=format&fit=crop', 6);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (7, 'Volo Roma - Londra', 'Volo di linea. Perfetto per raggiungere la conferenza internazionale di medicina d''urgenza.', 180.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 5);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (7, 'FCO', 'LHR', 'Roma', 'Londra', CURRENT_DATE + 20 + TIME '08:00:00', CURRENT_DATE + 20 + TIME '10:30:00', 30, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (13, 7, 'Economy', 180.00, 24), (14, 7, 'Business', 390.00, 6);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1513628253939-010e64ac66cd?q=80&w=1000&auto=format&fit=crop', 7),
                                                            ('https://images.unsplash.com/photo-1520106212299-d99c443e4568?q=80&w=1000&auto=format&fit=crop', 7);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (8, 'Volo Milano - Parigi', 'Volo diretto Malpensa - Charles de Gaulle, più frequenze giornaliere.', 145.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (8, 'MXP', 'CDG', 'Milano', 'Parigi', CURRENT_DATE + 25 + TIME '07:00:00', CURRENT_DATE + 25 + TIME '08:40:00', 42, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (15, 8, 'Economy', 145.00, 34), (16, 8, 'Business', 320.00, 8);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1502602898657-3e91760cbb34?q=80&w=1000&auto=format&fit=crop', 8),
                                                            ('https://images.unsplash.com/photo-1499856871958-5b9627545d1a?q=80&w=1000&auto=format&fit=crop', 8);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (9, 'Volo Napoli - Barcellona', 'Volo diretto verso la Catalogna, tariffa low-cost.', 99.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 3);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (9, 'NAP', 'BCN', 'Napoli', 'Barcellona', CURRENT_DATE + 10 + TIME '13:45:00', CURRENT_DATE + 10 + TIME '15:40:00', 38, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (17, 9, 'Economy', 99.00, 30), (18, 9, 'Business', 220.00, 8);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1523531294919-4bcd7c65e216?q=80&w=1000&auto=format&fit=crop', 9),
                                                            ('https://images.unsplash.com/photo-1583422409516-2895a77efded?q=80&w=1000&auto=format&fit=crop', 9);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (10, 'Volo Roma - New York', 'Volo diretto operato da ITA Airways. Include bagaglio in stiva.', 550.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (10, 'FCO', 'JFK', 'Roma', 'New York', CURRENT_DATE + 18 + TIME '10:00:00', CURRENT_DATE + 18 + TIME '14:00:00', 120, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (19, 10, 'Economy', 550.00, 96), (20, 10, 'Business', 1200.00, 24);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1499591934245-40b55745b905?q=80&w=1000&auto=format&fit=crop', 10),
                                                            ('https://images.unsplash.com/photo-1542296332-2e4473faf563?q=80&w=1000&auto=format&fit=crop', 10);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (11, 'Volo Milano - Lamezia Terme', 'Volo low cost per scendere giù al sud. Solo bagaglio a mano.', 45.99, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 3);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (11, 'MXP', 'SUF', 'Milano', 'Lamezia Terme', CURRENT_DATE + 7 + TIME '18:30:00', CURRENT_DATE + 7 + TIME '20:15:00', 45, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (21, 11, 'Economy', 45.99, 37), (22, 11, 'Business', 110.00, 8);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1530521954074-e64f6810b32d?q=80&w=1000&auto=format&fit=crop', 11),
                                                            ('https://images.unsplash.com/photo-1506012787146-f92b2d7d6d96?q=80&w=1000&auto=format&fit=crop', 11);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (12, 'Volo Verona - Trieste', 'Volo regionale, comodo per collegare Nord-Est e Nord-Ovest.', 59.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 3);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (12, 'VRN', 'TRS', 'Verona', 'Trieste', CURRENT_DATE + 4 + TIME '09:00:00', CURRENT_DATE + 4 + TIME '09:55:00', 28, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (23, 12, 'Economy', 59.00, 22), (24, 12, 'Business', 135.00, 6);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1595425964272-5b6a0cb6b6e3?q=80&w=1000&auto=format&fit=crop', 12),
                                                            ('https://images.unsplash.com/photo-1591810521626-9d3877dd0e0b?q=80&w=1000&auto=format&fit=crop', 12);

-- ==========================================
-- HOTEL (id 13-30)
-- Ogni hotel ha 2-3 RoomType con prezzo/capienza/benefit propri: "Da EUR X" in ricerca
-- e' il minimo tra queste. La disponibilita' vera si calcola per notte sui RoomHold.
-- ==========================================

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (13, 'Hotel Hilton Times Square', 'Soggiorno di lusso nel cuore di Manhattan con vista panoramica.', 250.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 5);
INSERT INTO hotel_details (id, location_lat, location_lng, address, city) VALUES
    (13, 40.7589, -73.9851, '234 W 42nd St, New York, NY 10036', 'New York');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (13, 'Wi-Fi'), (13, 'Palestra'), (13, 'Room Service'), (13, 'Aria Condizionata');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (1, 13, 'Doppia Deluxe', 'Camera doppia con vista sulla città.', 250.00, 10, 2),
    (2, 13, 'Suite Panoramica', 'Suite con vista panoramica su Manhattan, minibar e accappatoio.', 420.00, 5, 3),
    (3, 13, 'Tripla Family', 'Camera tripla ideale per famiglie, con letto aggiuntivo.', 310.00, 3, 3);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (1, 'Vista città'), (1, 'Aria condizionata'), (2, 'Vista panoramica'), (2, 'Minibar'), (2, 'Accappatoio'), (3, 'Letto aggiuntivo'), (3, 'Ideale famiglie');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (1, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000&auto=format&fit=crop'), (2, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1000&auto=format&fit=crop'), (3, 'https://images.unsplash.com/photo-1595576508898-0ad5c879a061?q=80&w=1000&auto=format&fit=crop');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (40, 13, 'Singola Executive', 'Camera singola compatta, ideale per viaggi di lavoro.', 165.00, 4, 1);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (40, 'Scrivania'), (40, 'Vista città');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (40, 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?q=80&w=1000&auto=format&fit=crop');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1566073771259-6a8506099945?q=80&w=1000&auto=format&fit=crop', 13),
                                                            ('https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?q=80&w=1000&auto=format&fit=crop', 13),
                                                            ('https://images.unsplash.com/photo-1582719478250-c89402bb6539?q=80&w=1000&auto=format&fit=crop', 13);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (14, 'Campus Relax Hotel', 'Struttura moderna a due passi dall''Università. Wi-Fi veloce e area studio.', 65.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, address, city) VALUES
    (14, 39.3615, 16.2285, 'Via Pietro Bucci, 87036 Rende (CS)', 'Rende');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (14, 'Wi-Fi'), (14, 'Area Studio'), (14, 'Parcheggio');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (4, 14, 'Singola Studenti', 'Camera singola con scrivania, pensata per soggiorni di studio.', 65.00, 6, 1),
    (5, 14, 'Doppia Studio', 'Camera doppia con angolo cottura e scrivania doppia.', 95.00, 2, 2);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (4, 'Scrivania'), (4, 'Wi-Fi veloce'), (5, 'Angolo cottura'), (5, 'Scrivania doppia');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (4, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000&auto=format&fit=crop'), (5, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1000&auto=format&fit=crop');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1555854877-bab0e564b8d5?q=80&w=1000&auto=format&fit=crop', 14),
                                                            ('https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?q=80&w=1000&auto=format&fit=crop', 14),
                                                            ('https://images.unsplash.com/photo-1497366216548-37526070297c?q=80&w=1000&auto=format&fit=crop', 14);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (15, 'Iron & Spa Resort', 'Hotel con palestra attrezzatissima per powerlifting, rack professionali e area benessere.', 120.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 5);
INSERT INTO hotel_details (id, location_lat, location_lng, address, city) VALUES
    (15, 45.4642, 9.1900, 'Via Roma 10, 20121 Milano', 'Milano');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (15, 'Wi-Fi'), (15, 'Palestra'), (15, 'Spa'), (15, 'Piscina');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (6, 15, 'Doppia Fitness', 'Camera doppia con accesso illimitato alla palestra.', 120.00, 2, 2),
    (7, 15, 'Suite con Pesi Liberi', 'Suite con rack e pesi liberi in camera, accesso spa incluso.', 190.00, 1, 2);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (6, 'Accesso palestra 24h'), (7, 'Rack personale in camera'), (7, 'Accesso spa incluso');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (6, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000&auto=format&fit=crop'), (7, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1000&auto=format&fit=crop');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (41, 15, 'Singola Fitness', 'Camera singola con accesso alla palestra.', 75.00, 3, 1);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (41, 'Accesso palestra 24h');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (41, 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?q=80&w=1000&auto=format&fit=crop');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1534438327276-14e5300c3a48?q=80&w=1000&auto=format&fit=crop', 15),
                                                            ('https://images.unsplash.com/photo-1571902943202-507ec2618e8f?q=80&w=1000&auto=format&fit=crop', 15),
                                                            ('https://images.unsplash.com/photo-1540497077202-7c8a3999166f?q=80&w=1000&auto=format&fit=crop', 15);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (16, 'Art & Ink Boutique Hotel', 'Struttura dal design post-industriale. Al piano terra si trova uno studio di tatuatori residenti.', 95.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, address, city) VALUES
    (16, 41.9028, 12.4964, 'Via del Corso 45, 00186 Roma', 'Roma');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (16, 'Wi-Fi'), (16, 'Bar'), (16, 'Parcheggio');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (8, 16, 'Loft Industriale', 'Loft open space con arredi post-industriali.', 95.00, 3, 2),
    (9, 16, 'Loft con Terrazzo', 'Loft con terrazzo privato e opera d''arte originale.', 150.00, 2, 3);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (8, 'Design open space'), (9, 'Terrazzo privato'), (9, 'Opera d''arte originale');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (8, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000&auto=format&fit=crop'), (9, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1000&auto=format&fit=crop');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (42, 16, 'Singola Artistica', 'Camera singola con arredi post-industriali.', 65.00, 2, 1);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (42, 'Design open space');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (42, 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?q=80&w=1000&auto=format&fit=crop');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1560066984-138dadb4c035?q=80&w=1000&auto=format&fit=crop', 16),
                                                            ('https://images.unsplash.com/photo-1598331668904-45ea0f3b4d45?q=80&w=1000&auto=format&fit=crop', 16),
                                                            ('https://images.unsplash.com/photo-1512406830500-1c05000570fc?q=80&w=1000&auto=format&fit=crop', 16);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (17, 'Gamer''s Haven Lodge', 'Hotel dedicato agli eSports. Connessione fibra dedicata, postazioni PC in camera, ideale per raid notturni.', 110.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, address, city) VALUES
    (17, 52.5200, 13.4050, 'Alexanderplatz 5, 10178 Berlin', 'Berlino');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (17, 'Wi-Fi'), (17, 'Fibra Dedicata'), (17, 'Aria Condizionata');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (10, 17, 'Gaming Room Singola', 'Camera singola con una postazione PC gaming.', 110.00, 7, 1),
    (11, 17, 'Gaming Suite 2 Postazioni', 'Suite con due postazioni PC gaming e monitor 240Hz.', 175.00, 5, 2);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (10, 'PC gaming'), (10, 'Sedia ergonomica'), (11, '2 postazioni PC'), (11, 'Monitor 240Hz');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (10, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000&auto=format&fit=crop'), (11, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1000&auto=format&fit=crop');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1542751371-adc38448a05e?q=80&w=1000&auto=format&fit=crop', 17),
                                                            ('https://images.unsplash.com/photo-1593305841991-05c297ba4575?q=80&w=1000&auto=format&fit=crop', 17),
                                                            ('https://images.unsplash.com/photo-1538481199705-c710c4e965fc?q=80&w=1000&auto=format&fit=crop', 17);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (18, 'Palazzo Vecchio Suites', 'Dimora storica restaurata nel centro di Firenze, a due passi dagli Uffizi.', 210.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 5);
INSERT INTO hotel_details (id, location_lat, location_lng, address, city) VALUES
    (18, 43.7696, 11.2558, 'Via dei Calzaiuoli 12, 50122 Firenze', 'Firenze');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (18, 'Wi-Fi'), (18, 'Colazione Inclusa'), (18, 'Room Service'), (18, 'Aria Condizionata');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (12, 18, 'Doppia Rinascimentale', 'Camera doppia con affreschi originali.', 210.00, 4, 2),
    (13, 18, 'Suite Rinascimentale', 'Suite con salotto separato e vista sul Duomo.', 340.00, 2, 3);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (12, 'Affreschi originali'), (13, 'Salotto separato'), (13, 'Vista Duomo');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (12, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000&auto=format&fit=crop'), (13, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1000&auto=format&fit=crop');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (43, 18, 'Singola Rinascimentale', 'Camera singola con affreschi originali.', 135.00, 3, 1);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (43, 'Affreschi originali');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (43, 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?q=80&w=1000&auto=format&fit=crop');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1541971875076-8f970d573be6?q=80&w=1000&auto=format&fit=crop', 18),
                                                            ('https://images.unsplash.com/photo-1445019980597-93fa8acb246c?q=80&w=1000&auto=format&fit=crop', 18),
                                                            ('https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?q=80&w=1000&auto=format&fit=crop', 18);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (19, 'Canal Grande Boutique Hotel', 'Vista diretta sul Canal Grande, arredi veneziani originali del XVIII secolo.', 280.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 5);
INSERT INTO hotel_details (id, location_lat, location_lng, address, city) VALUES
    (19, 45.4408, 12.3155, 'Fondamenta del Vin 34, 30124 Venezia', 'Venezia');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (19, 'Wi-Fi'), (19, 'Colazione Inclusa'), (19, 'Bar');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (14, 19, 'Camera Vista Interna', 'Camera doppia con arredi veneziani originali del XVIII secolo.', 280.00, 2, 2),
    (15, 19, 'Camera Vista Canale', 'Camera con vista diretta sul Canal Grande e balconcino.', 390.00, 2, 2);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (14, 'Arredi originali XVIII secolo'), (15, 'Vista diretta Canal Grande'), (15, 'Balconcino');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (14, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000&auto=format&fit=crop'), (15, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1000&auto=format&fit=crop');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (44, 19, 'Singola Vista Interna', 'Camera singola con arredi veneziani originali.', 190.00, 2, 1);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (44, 'Arredi originali XVIII secolo');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (44, 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?q=80&w=1000&auto=format&fit=crop');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1523906834658-6e24ef2386f9?q=80&w=1000&auto=format&fit=crop', 19),
                                                            ('https://images.unsplash.com/photo-1534113414509-0eec2bfb493f?q=80&w=1000&auto=format&fit=crop', 19),
                                                            ('https://images.unsplash.com/photo-1514890547357-a9ee288728e0?q=80&w=1000&auto=format&fit=crop', 19);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (20, 'Portici Rossi Hotel', 'Hotel elegante sotto i portici del centro storico di Bologna, vicino alle Due Torri.', 130.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, address, city) VALUES
    (20, 44.4949, 11.3426, 'Via Rizzoli 8, 40125 Bologna', 'Bologna');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (20, 'Wi-Fi'), (20, 'Colazione Inclusa'), (20, 'Parcheggio');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (16, 20, 'Doppia Classic', 'Camera doppia con vista sui portici.', 130.00, 7, 2),
    (17, 20, 'Doppia Superior', 'Camera doppia con vista sulle Due Torri e minibar.', 175.00, 3, 2);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (16, 'Vista portici'), (17, 'Vista Due Torri'), (17, 'Minibar');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (16, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000&auto=format&fit=crop'), (17, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1000&auto=format&fit=crop');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (45, 20, 'Singola Classic', 'Camera singola sotto i portici del centro storico.', 85.00, 3, 1);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (45, 'Vista portici');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (45, 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?q=80&w=1000&auto=format&fit=crop');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1555400038-63f5ba517a47?q=80&w=1000&auto=format&fit=crop', 20),
                                                            ('https://images.unsplash.com/photo-1445019980597-93fa8acb246c?q=80&w=1000&auto=format&fit=crop', 20),
                                                            ('https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?q=80&w=1000&auto=format&fit=crop', 20);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (21, 'Mondello Beach Resort', 'Resort fronte mare a Mondello, con spiaggia privata e piscina panoramica.', 165.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 5);
INSERT INTO hotel_details (id, location_lat, location_lng, address, city) VALUES
    (21, 38.1938, 13.3266, 'Viale Regina Elena 45, 90151 Palermo', 'Palermo');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (21, 'Wi-Fi'), (21, 'Piscina'), (21, 'Spiaggia Privata'), (21, 'Colazione Inclusa');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (18, 21, 'Camera Vista Giardino', 'Camera doppia con accesso piscina.', 165.00, 12, 2),
    (19, 21, 'Camera Vista Mare', 'Camera con vista mare diretta e accesso alla spiaggia privata.', 225.00, 8, 3),
    (20, 21, 'Tripla Family Vista Mare', 'Camera tripla vista mare con letto aggiuntivo, ideale famiglie.', 270.00, 4, 3);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (18, 'Accesso piscina'), (19, 'Vista mare diretta'), (19, 'Accesso spiaggia privata'), (20, 'Letto aggiuntivo'), (20, 'Vista mare');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (18, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000&auto=format&fit=crop'), (19, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1000&auto=format&fit=crop'), (20, 'https://images.unsplash.com/photo-1595576508898-0ad5c879a061?q=80&w=1000&auto=format&fit=crop');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (46, 21, 'Singola Vista Giardino', 'Camera singola con accesso piscina.', 105.00, 4, 1);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (46, 'Accesso piscina');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (46, 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?q=80&w=1000&auto=format&fit=crop');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?q=80&w=1000&auto=format&fit=crop', 21),
                                                            ('https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?q=80&w=1000&auto=format&fit=crop', 21),
                                                            ('https://images.unsplash.com/photo-1519821172141-b5d8342c2a24?q=80&w=1000&auto=format&fit=crop', 21);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (22, 'Etna View Country House', 'Agriturismo panoramico alle pendici dell''Etna, immerso tra vigneti e uliveti.', 88.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, address, city) VALUES
    (22, 37.6100, 15.1500, 'Contrada Etna 3, 95030 Catania', 'Catania');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (22, 'Wi-Fi'), (22, 'Colazione Inclusa'), (22, 'Parcheggio'), (22, 'Piscina');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (21, 22, 'Camera Country', 'Camera doppia con vista sui vigneti.', 88.00, 5, 2),
    (22, 22, 'Camera Country Vista Etna', 'Camera con vista sull''Etna e terrazza privata.', 120.00, 2, 3);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (21, 'Vista vigneti'), (22, 'Vista Etna'), (22, 'Terrazza privata');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (21, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000&auto=format&fit=crop'), (22, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1000&auto=format&fit=crop');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (47, 22, 'Singola Country', 'Camera singola con vista sui vigneti.', 58.00, 3, 1);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (47, 'Vista vigneti');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (47, 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?q=80&w=1000&auto=format&fit=crop');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1502301103665-0b95cc738daf?q=80&w=1000&auto=format&fit=crop', 22),
                                                            ('https://images.unsplash.com/photo-1568605114967-8130f3a36994?q=80&w=1000&auto=format&fit=crop', 22),
                                                            ('https://images.unsplash.com/photo-1518733057094-95b53143d2a7?q=80&w=1000&auto=format&fit=crop', 22);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (23, 'Masseria dei Trulli', 'Masseria tradizionale pugliese convertita in hotel diffuso, vicino ad Alberobello.', 140.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 5);
INSERT INTO hotel_details (id, location_lat, location_lng, address, city) VALUES
    (23, 41.1177, 16.8719, 'Contrada Trulli 21, 70011 Alberobello (BA)', 'Bari');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (23, 'Wi-Fi'), (23, 'Piscina'), (23, 'Colazione Inclusa'), (23, 'Parcheggio');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (23, 23, 'Trullo Classic', 'Trullo con soffitto a cono originale.', 140.00, 6, 2),
    (24, 23, 'Trullo Deluxe', 'Trullo con zona giorno separata e giardino privato.', 195.00, 3, 3);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (23, 'Soffitto a cono originale'), (24, 'Zona giorno separata'), (24, 'Giardino privato');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (23, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000&auto=format&fit=crop'), (24, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1000&auto=format&fit=crop');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (48, 23, 'Singola Trullo', 'Trullo singolo con soffitto a cono originale.', 95.00, 3, 1);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (48, 'Soffitto a cono originale');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (48, 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?q=80&w=1000&auto=format&fit=crop');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1523731407965-2430cd12f5e4?q=80&w=1000&auto=format&fit=crop', 23),
                                                            ('https://images.unsplash.com/photo-1587213811864-73fdb3617d7c?q=80&w=1000&auto=format&fit=crop', 23),
                                                            ('https://images.unsplash.com/photo-1595877244574-e90ce41ce089?q=80&w=1000&auto=format&fit=crop', 23);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (24, 'Porto Antico Hotel', 'Hotel moderno affacciato sul Porto Antico di Genova, a due passi dall''Acquario.', 105.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, address, city) VALUES
    (24, 44.4056, 8.9463, 'Calata Cattaneo 2, 16126 Genova', 'Genova');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (24, 'Wi-Fi'), (24, 'Room Service'), (24, 'Parcheggio');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (25, 24, 'Doppia Standard', 'Camera doppia con vista città.', 105.00, 7, 2),
    (26, 24, 'Doppia Vista Porto', 'Camera doppia con vista sul Porto Antico.', 145.00, 4, 2);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (25, 'Vista città'), (26, 'Vista Porto Antico');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (25, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000&auto=format&fit=crop'), (26, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1000&auto=format&fit=crop');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (49, 24, 'Singola Standard', 'Camera singola con vista città.', 70.00, 4, 1);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (49, 'Vista città');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (49, 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?q=80&w=1000&auto=format&fit=crop');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?q=80&w=1000&auto=format&fit=crop', 24),
                                                            ('https://images.unsplash.com/photo-1455587734955-081b22074882?q=80&w=1000&auto=format&fit=crop', 24),
                                                            ('https://images.unsplash.com/photo-1571896349842-33c89424de2d?q=80&w=1000&auto=format&fit=crop', 24);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (25, 'Verona Romantica Hotel', 'Hotel boutique a pochi passi da Casa di Giulietta, arredamento romantico.', 115.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, address, city) VALUES
    (25, 45.4384, 10.9916, 'Via Cappello 20, 37121 Verona', 'Verona');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (25, 'Wi-Fi'), (25, 'Colazione Inclusa'), (25, 'Bar');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (27, 25, 'Doppia Romantica', 'Camera doppia con arredamento romantico.', 115.00, 5, 2),
    (28, 25, 'Camera Romeo e Giulietta', 'Camera con vista sulla Casa di Giulietta e champagne di benvenuto.', 165.00, 3, 2);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (27, 'Arredamento romantico'), (28, 'Vista Casa di Giulietta'), (28, 'Champagne di benvenuto');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (27, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000&auto=format&fit=crop'), (28, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1000&auto=format&fit=crop');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (50, 25, 'Singola Romantica', 'Camera singola con arredamento romantico.', 75.00, 3, 1);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (50, 'Arredamento romantico');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (50, 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?q=80&w=1000&auto=format&fit=crop');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1555400038-63f5ba517a47?q=80&w=1000&auto=format&fit=crop', 25),
                                                            ('https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?q=80&w=1000&auto=format&fit=crop', 25),
                                                            ('https://images.unsplash.com/photo-1445019980597-93fa8acb246c?q=80&w=1000&auto=format&fit=crop', 25);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (26, 'Poetto Beach Hotel', 'Hotel a due passi dalla spiaggia del Poetto, con terrazza panoramica sul golfo.', 98.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, address, city) VALUES
    (26, 39.2103, 9.1547, 'Viale Poetto 100, 09126 Cagliari', 'Cagliari');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (26, 'Wi-Fi'), (26, 'Colazione Inclusa'), (26, 'Parcheggio'), (26, 'Bar');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (29, 26, 'Camera Standard', 'Camera doppia con accesso alla terrazza comune.', 98.00, 9, 2),
    (30, 26, 'Camera Vista Golfo', 'Camera con vista sul golfo e balcone privato.', 135.00, 5, 3);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (29, 'Terrazza comune'), (30, 'Vista golfo'), (30, 'Balcone privato');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (29, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000&auto=format&fit=crop'), (30, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1000&auto=format&fit=crop');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (51, 26, 'Singola Standard', 'Camera singola con accesso alla terrazza comune.', 65.00, 4, 1);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (51, 'Terrazza comune');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (51, 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?q=80&w=1000&auto=format&fit=crop');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1519046904884-53103b34b206?q=80&w=1000&auto=format&fit=crop', 26),
                                                            ('https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?q=80&w=1000&auto=format&fit=crop', 26),
                                                            ('https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?q=80&w=1000&auto=format&fit=crop', 26);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (27, 'Barocco Salentino Hotel', 'Palazzo barocco restaurato nel centro storico di Lecce, la "Firenze del Sud".', 125.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 5);
INSERT INTO hotel_details (id, location_lat, location_lng, address, city) VALUES
    (27, 40.3515, 18.1750, 'Via Palmieri 15, 73100 Lecce', 'Lecce');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (27, 'Wi-Fi'), (27, 'Colazione Inclusa'), (27, 'Room Service');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (31, 27, 'Doppia Barocca', 'Camera doppia con soffitti affrescati.', 125.00, 4, 2),
    (32, 27, 'Suite Barocca', 'Suite con salotto privato e vista sul centro storico.', 190.00, 2, 3);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (31, 'Soffitti affrescati'), (32, 'Salotto privato'), (32, 'Vista centro storico');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (31, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000&auto=format&fit=crop'), (32, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1000&auto=format&fit=crop');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (52, 27, 'Singola Barocca', 'Camera singola con soffitti affrescati.', 80.00, 3, 1);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (52, 'Soffitti affrescati');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (52, 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?q=80&w=1000&auto=format&fit=crop');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1541971875076-8f970d573be6?q=80&w=1000&auto=format&fit=crop', 27),
                                                            ('https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?q=80&w=1000&auto=format&fit=crop', 27),
                                                            ('https://images.unsplash.com/photo-1587213811864-73fdb3617d7c?q=80&w=1000&auto=format&fit=crop', 27);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (28, 'Grand Hotel Rimini Mare', 'Hotel storico fronte mare a Rimini, con stabilimento balneare privato.', 135.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, address, city) VALUES
    (28, 44.0678, 12.5695, 'Parco Federico Fellini 1, 47921 Rimini', 'Rimini');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (28, 'Wi-Fi'), (28, 'Piscina'), (28, 'Spiaggia Privata'), (28, 'Colazione Inclusa');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (33, 28, 'Camera Vista Città', 'Camera doppia con accesso piscina.', 135.00, 16, 2),
    (34, 28, 'Camera Vista Mare', 'Camera con vista mare e accesso allo stabilimento privato.', 185.00, 9, 3),
    (35, 28, 'Tripla Family', 'Camera tripla con letto aggiuntivo, ideale per famiglie.', 230.00, 5, 3);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (33, 'Accesso piscina'), (34, 'Vista mare'), (34, 'Accesso stabilimento privato'), (35, 'Letto aggiuntivo'), (35, 'Ideale famiglie');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (33, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000&auto=format&fit=crop'), (34, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1000&auto=format&fit=crop'), (35, 'https://images.unsplash.com/photo-1595576508898-0ad5c879a061?q=80&w=1000&auto=format&fit=crop');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (53, 28, 'Singola Vista Città', 'Camera singola con accesso piscina.', 90.00, 6, 1);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (53, 'Accesso piscina');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (53, 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?q=80&w=1000&auto=format&fit=crop');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1519046904884-53103b34b206?q=80&w=1000&auto=format&fit=crop', 28),
                                                            ('https://images.unsplash.com/photo-1519821172141-b5d8342c2a24?q=80&w=1000&auto=format&fit=crop', 28),
                                                            ('https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?q=80&w=1000&auto=format&fit=crop', 28);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (29, 'Torre Pendente Hotel', 'Hotel a 5 minuti a piedi dalla Torre di Pisa, con vista su Piazza dei Miracoli.', 112.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, address, city) VALUES
    (29, 43.7228, 10.3966, 'Via Santa Maria 55, 56126 Pisa', 'Pisa');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (29, 'Wi-Fi'), (29, 'Colazione Inclusa'), (29, 'Bar');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (36, 29, 'Doppia Standard', 'Camera doppia nel centro storico.', 112.00, 6, 2),
    (37, 29, 'Camera Vista Torre', 'Camera con vista sulla Torre di Pisa.', 155.00, 3, 2);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (36, 'Centro storico'), (37, 'Vista Torre di Pisa');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (36, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000&auto=format&fit=crop'), (37, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1000&auto=format&fit=crop');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (54, 29, 'Singola Standard', 'Camera singola nel centro storico.', 72.00, 3, 1);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (54, 'Centro storico');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (54, 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?q=80&w=1000&auto=format&fit=crop');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1543429257-3e7c2c6d4d9b?q=80&w=1000&auto=format&fit=crop', 29),
                                                            ('https://images.unsplash.com/photo-1541971875076-8f970d573be6?q=80&w=1000&auto=format&fit=crop', 29),
                                                            ('https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?q=80&w=1000&auto=format&fit=crop', 29);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (30, 'Contrada Palio Hotel', 'Hotel nel cuore di Siena, sulla storica Piazza del Campo, patria del Palio.', 128.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 5);
INSERT INTO hotel_details (id, location_lat, location_lng, address, city) VALUES
    (30, 43.3188, 11.3308, 'Piazza del Campo 30, 53100 Siena', 'Siena');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (30, 'Wi-Fi'), (30, 'Colazione Inclusa'), (30, 'Room Service');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (38, 30, 'Doppia Standard', 'Camera doppia nel centro storico di Siena.', 128.00, 4, 2),
    (39, 30, 'Camera Vista Piazza', 'Camera con vista su Piazza del Campo.', 175.00, 3, 2);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (38, 'Centro storico Siena'), (39, 'Vista Piazza del Campo');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (38, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000&auto=format&fit=crop'), (39, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1000&auto=format&fit=crop');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (55, 30, 'Singola Standard', 'Camera singola nel centro storico di Siena.', 82.00, 3, 1);
INSERT INTO room_type_benefits (room_type_id, benefit) VALUES (55, 'Centro storico Siena');
INSERT INTO room_type_images (room_type_id, image_url) VALUES (55, 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?q=80&w=1000&auto=format&fit=crop');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1541971875076-8f970d573be6?q=80&w=1000&auto=format&fit=crop', 30),
                                                            ('https://images.unsplash.com/photo-1445019980597-93fa8acb246c?q=80&w=1000&auto=format&fit=crop', 30),
                                                            ('https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?q=80&w=1000&auto=format&fit=crop', 30);

-- ==========================================
-- ATTIVITÀ (id 31-40)
-- ==========================================

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (31, 'Trekking Impegnativo in Sila', 'Percorso avanzato nei boschi silani. Ottimo per testare la propria resistenza e staccare la spina dallo schermo.', 45.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Attività', 5);
INSERT INTO activity_details (id, activity_type, duration, meeting_point, city, max_participants, guide_included) VALUES
    (31, 'Sport e Natura', '6 ore', 'Centro Visite Cupone, Parco Nazionale della Sila', 'Sila', 15, true);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1551632811-561f32228f3c?q=80&w=1000&auto=format&fit=crop', 31),
                                                            ('https://images.unsplash.com/photo-1470071131384-001b85755536?q=80&w=1000&auto=format&fit=crop', 31);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (32, 'Evento Fantasy: Difesa del Castello', 'Giornata immersiva di gioco di ruolo dal vivo (LARP) in un borgo medievale. Scegli la tua fazione, farma risorse e difendi l''avamposto!', 30.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Attività', 4);
INSERT INTO activity_details (id, activity_type, duration, meeting_point, city, max_participants, guide_included) VALUES
    (32, 'Evento dal Vivo', 'Giornata intera', 'Castello Svevo, Rocca Imperiale', 'Rocca Imperiale', 50, false);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1605806616949-1e87b487cb2a?q=80&w=1000&auto=format&fit=crop', 32),
                                                            ('https://images.unsplash.com/photo-1590055531615-f16d36ffe8ea?q=80&w=1000&auto=format&fit=crop', 32);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (33, 'Tour Guidato del Colosseo', 'Visita guidata al Colosseo, Foro Romano e Palatino con accesso salta-fila.', 55.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Attività', 5);
INSERT INTO activity_details (id, activity_type, duration, meeting_point, city, max_participants, guide_included) VALUES
    (33, 'Cultura e Storia', '3 ore', 'Piazza del Colosseo, ingresso principale', 'Roma', 20, true);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1552832230-c0197dd311b5?q=80&w=1000&auto=format&fit=crop', 33),
                                                            ('https://images.unsplash.com/photo-1531572753322-ad063cecc140?q=80&w=1000&auto=format&fit=crop', 33);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (34, 'Giro in Gondola sul Canal Grande', 'Romantico giro in gondola tra i canali di Venezia con gondoliere esperto.', 80.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Attività', 5);
INSERT INTO activity_details (id, activity_type, duration, meeting_point, city, max_participants, guide_included) VALUES
    (34, 'Esperienza Romantica', '40 minuti', 'Bacino Orseolo, vicino Piazza San Marco', 'Venezia', 6, true);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1514890547357-a9ee288728e0?q=80&w=1000&auto=format&fit=crop', 34),
                                                            ('https://images.unsplash.com/photo-1534113414509-0eec2bfb493f?q=80&w=1000&auto=format&fit=crop', 34);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (35, 'Degustazione Vini in Chianti', 'Tour tra le vigne del Chianti con degustazione di 5 vini e piatti tipici toscani.', 65.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Attività', 5);
INSERT INTO activity_details (id, activity_type, duration, meeting_point, city, max_participants, guide_included) VALUES
    (35, 'Enogastronomia', '4 ore', 'Cantina San Donato, Greve in Chianti', 'Chianti', 12, true);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1506377247377-2a5b3b417ebb?q=80&w=1000&auto=format&fit=crop', 35),
                                                            ('https://images.unsplash.com/photo-1516594798947-e65505dbb29d?q=80&w=1000&auto=format&fit=crop', 35);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (36, 'Escursione in Barca alle Cinque Terre', 'Giornata in barca lungo la costa delle Cinque Terre con soste bagno a Vernazza e Monterosso.', 70.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Attività', 5);
INSERT INTO activity_details (id, activity_type, duration, meeting_point, city, max_participants, guide_included) VALUES
    (36, 'Sport e Natura', '7 ore', 'Porto di La Spezia, Molo Italia', 'Cinque Terre', 18, true);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1519681393784-d120267933ba?q=80&w=1000&auto=format&fit=crop', 36),
                                                            ('https://images.unsplash.com/photo-1543429776-2782fc8e1acd?q=80&w=1000&auto=format&fit=crop', 36);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (37, 'Trekking sul Vesuvio', 'Salita guidata al cratere del Vesuvio con vista panoramica sul Golfo di Napoli.', 40.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Attività', 4);
INSERT INTO activity_details (id, activity_type, duration, meeting_point, city, max_participants, guide_included) VALUES
    (37, 'Sport e Natura', '3 ore', 'Quota 1000, ingresso Parco Nazionale del Vesuvio', 'Napoli', 25, true);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1533104816931-20fa691ff6ca?q=80&w=1000&auto=format&fit=crop', 37),
                                                            ('https://images.unsplash.com/photo-1518709594023-6eab9bab7b23?q=80&w=1000&auto=format&fit=crop', 37);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (38, 'Escursione al Cratere dell''Etna', 'Trekking guidato sui crateri sommitali dell''Etna con guida alpina certificata.', 95.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Attività', 5);
INSERT INTO activity_details (id, activity_type, duration, meeting_point, city, max_participants, guide_included) VALUES
    (38, 'Sport e Natura', '8 ore', 'Rifugio Sapienza, Quota 1900', 'Catania', 10, true);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1591604129939-f1efa4d9f7fa?q=80&w=1000&auto=format&fit=crop', 38),
                                                            ('https://images.unsplash.com/photo-1523731407965-2430cd12f5e4?q=80&w=1000&auto=format&fit=crop', 38);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (39, 'Tour dei Trulli di Alberobello', 'Passeggiata guidata tra i trulli patrimonio UNESCO con laboratorio di cucina pugliese.', 50.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Attività', 5);
INSERT INTO activity_details (id, activity_type, duration, meeting_point, city, max_participants, guide_included) VALUES
    (39, 'Cultura e Storia', '3 ore', 'Largo Martellotta, centro storico', 'Alberobello', 16, true);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1587213811864-73fdb3617d7c?q=80&w=1000&auto=format&fit=crop', 39),
                                                            ('https://images.unsplash.com/photo-1595877244574-e90ce41ce089?q=80&w=1000&auto=format&fit=crop', 39);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (40, 'Immersione Subacquea a Cala Gonone', 'Battesimo del mare o immersione guidata nelle acque cristalline della Sardegna.', 75.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Attività', 5);
INSERT INTO activity_details (id, activity_type, duration, meeting_point, city, max_participants, guide_included) VALUES
    (40, 'Sport e Natura', '2 ore', 'Diving Center, Porto di Cala Gonone', 'Cagliari', 8, true);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1544551763-46a013bb70d5?q=80&w=1000&auto=format&fit=crop', 40),
                                                            ('https://images.unsplash.com/photo-1544551763-77ef2d0cfc6c?q=80&w=1000&auto=format&fit=crop', 40);

-- ==========================================
-- COMPONENTI MANCANTI PER ITINERARI MULTI-TAPPA (New York / Toronto)
-- ==========================================

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (41, 'Tour a piedi di Manhattan', 'Cammina tra Times Square, Central Park ed Empire State Building con una guida locale.', 55.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Attività', 5);
INSERT INTO activity_details (id, activity_type, duration, meeting_point, city, max_participants, guide_included) VALUES
    (41, 'Tour Urbano', '3 ore', 'Times Square, angolo 7th Ave', 'New York', 15, true);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
    ('https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?q=80&w=1000&auto=format&fit=crop', 41);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (42, 'Hotel Fairmont Royal York Toronto', 'Hotel storico nel cuore della downtown di Toronto, a due passi dalla CN Tower.', 210.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 5);
INSERT INTO hotel_details (id, location_lat, location_lng, address, city) VALUES
    (42, 43.6453, -79.3806, '100 Front St W, Toronto, ON M5J 1E3', 'Toronto');
INSERT INTO room_types (id, hotel_id, name, description, price, total_rooms, max_occupancy) VALUES
    (56, 42, 'Doppia Classic', 'Camera doppia con vista sulla città.', 210.00, 12, 2),
    (57, 42, 'Suite Royal', 'Suite con salotto separato e vista sul lago Ontario.', 380.00, 4, 3);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
    ('https://images.unsplash.com/photo-1517840901100-8179e982acb7?q=80&w=1000&auto=format&fit=crop', 42);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (43, 'Tour delle Cascate del Niagara da Toronto', 'Giornata alle Cascate del Niagara con crociera Hornblower inclusa, partenza da Toronto.', 95.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Attività', 5);
INSERT INTO activity_details (id, activity_type, duration, meeting_point, city, max_participants, guide_included) VALUES
    (43, 'Escursione', '8 ore', 'Union Station, Toronto', 'Toronto', 20, true);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
    ('https://images.unsplash.com/photo-1489447068241-b3490214e879?q=80&w=1000&auto=format&fit=crop', 43);

-- ==========================================
-- VOLI ANDATA/RITORNO PER GLI ITINERARI DI ESEMPIO
-- I voli one-off 1-12 sono tutti a senso unico (nessuno di essi ha un ritorno reale):
-- un itinerario costruito con quelli non può mai essere coerente (nessun volo riporta
-- a casa). Queste coppie dedicate rendono i 5 itinerari di esempio realmente validi:
-- stessa città di arrivo dell'andata = città di partenza del ritorno, hotel/attività
-- nella stessa città di quella tappa.
-- ==========================================

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (44, 'Volo Milano - Roma', 'Andata per il weekend nella Capitale.', 82.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (44, 'LIN', 'FCO', 'Milano', 'Roma', '2026-09-03 08:00:00', '2026-09-03 09:15:00', 60, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (25, 44, 'Economy', 82.00, 48), (26, 44, 'Business', 190.00, 12);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (45, 'Volo Roma - Milano (rientro)', 'Rientro a Milano dopo il weekend romano.', 89.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (45, 'FCO', 'LIN', 'Roma', 'Milano', '2026-09-06 18:00:00', '2026-09-06 19:15:00', 60, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (27, 45, 'Economy', 89.00, 48), (28, 45, 'Business', 205.00, 12);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (46, 'Volo Roma - Venezia', 'Andata verso la Serenissima.', 76.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (46, 'FCO', 'VCE', 'Roma', 'Venezia', '2026-09-10 09:00:00', '2026-09-10 10:10:00', 55, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (29, 46, 'Economy', 76.00, 44), (30, 46, 'Business', 175.00, 11);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (47, 'Volo Venezia - Roma (rientro)', 'Rientro dalla Serenissima.', 78.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (47, 'VCE', 'FCO', 'Venezia', 'Roma', '2026-09-13 19:00:00', '2026-09-13 20:10:00', 55, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (31, 47, 'Economy', 78.00, 44), (32, 47, 'Business', 180.00, 11);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (48, 'Volo Roma - Catania', 'Andata verso l''Etna.', 84.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (48, 'FCO', 'CTA', 'Roma', 'Catania', '2026-09-17 07:30:00', '2026-09-17 09:00:00', 55, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (33, 48, 'Economy', 84.00, 44), (34, 48, 'Business', 195.00, 11);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (49, 'Volo Catania - Roma (rientro)', 'Rientro dalla Sicilia.', 86.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (49, 'CTA', 'FCO', 'Catania', 'Roma', '2026-09-21 20:00:00', '2026-09-21 21:30:00', 55, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (35, 49, 'Economy', 86.00, 44), (36, 49, 'Business', 198.00, 11);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (50, 'Volo Roma - Bari', 'Andata verso la Puglia.', 74.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (50, 'FCO', 'BRI', 'Roma', 'Bari', '2026-09-24 08:15:00', '2026-09-24 09:30:00', 55, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (37, 50, 'Economy', 74.00, 44), (38, 50, 'Business', 170.00, 11);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (51, 'Volo Bari - Roma (rientro)', 'Rientro dalla Puglia.', 76.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (51, 'BRI', 'FCO', 'Bari', 'Roma', '2026-09-27 18:30:00', '2026-09-27 19:45:00', 55, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (39, 51, 'Economy', 76.00, 44), (40, 51, 'Business', 175.00, 11);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (52, 'Volo Roma - Cagliari', 'Andata verso la Sardegna.', 80.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (52, 'FCO', 'CAG', 'Roma', 'Cagliari', '2026-10-01 09:45:00', '2026-10-01 11:00:00', 55, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (41, 52, 'Economy', 80.00, 44), (42, 52, 'Business', 185.00, 11);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (53, 'Volo Cagliari - Roma (rientro)', 'Rientro dalla Sardegna.', 82.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops) VALUES
    (53, 'CAG', 'FCO', 'Cagliari', 'Roma', '2026-10-05 19:15:00', '2026-10-05 20:30:00', 55, 0);
INSERT INTO fare_classes (id, flight_id, name, price, total_seats) VALUES
    (43, 53, 'Economy', 82.00, 44), (44, 53, 'Business', 188.00, 11);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (54, 'Tour del Centro Storico di Bari', 'Passeggiata guidata nella Bari Vecchia, tra la Basilica di San Nicola e il lungomare.', 35.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Attività', 4);
INSERT INTO activity_details (id, activity_type, duration, meeting_point, city, max_participants, guide_included) VALUES
    (54, 'Cultura e Storia', '2 ore e mezza', 'Basilica di San Nicola, Bari Vecchia', 'Bari', 18, true);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
    ('https://images.unsplash.com/photo-1591604466107-ec97de577aff?q=80&w=1000&auto=format&fit=crop', 54);

-- ==========================================
-- VOLI RICORRENTI (Roma<->New York, New York<->Toronto, Toronto<->Roma)
-- Un volo al giorno per 60 giorni da oggi, così un itinerario costruito con
-- date scelte liberamente dall'utente trova sempre un volo compatibile.
-- Gli id vengono calcolati a partire dal MAX(id) attuale (catturato UNA sola
-- volta in una tabella temporanea) per non collidere con i dati sopra, anche
-- se in futuro si aggiungono altri item prima di questo blocco.
-- ==========================================

CREATE TEMP TABLE tmp_recurring_flights AS
WITH routes(route_order, dep_airport, arr_airport, dep_city, arr_city, dep_hour, flight_duration, base_price) AS (
    VALUES
        (0, 'FCO', 'JFK', 'Roma',    'New York', TIME '10:00', INTERVAL '8 hours',           550.00),
        (1, 'JFK', 'FCO', 'New York','Roma',     TIME '20:00', INTERVAL '8 hours 30 minutes', 560.00),
        (2, 'JFK', 'YYZ', 'New York','Toronto',  TIME '09:00', INTERVAL '1 hour 30 minutes',  180.00),
        (3, 'YYZ', 'JFK', 'Toronto', 'New York', TIME '18:00', INTERVAL '1 hour 30 minutes',  175.00),
        (4, 'YYZ', 'FCO', 'Toronto', 'Roma',     TIME '21:00', INTERVAL '9 hours',            590.00),
        (5, 'FCO', 'YYZ', 'Roma',    'Toronto',  TIME '11:00', INTERVAL '9 hours 30 minutes', 600.00)
),
days AS (
    SELECT generate_series(CURRENT_DATE + 1, CURRENT_DATE + 60, INTERVAL '1 day')::date AS the_day
)
SELECT
    (SELECT MAX(id) FROM catalog_items) + (route_order * 60)
        + (ROW_NUMBER() OVER (PARTITION BY route_order ORDER BY the_day)) AS item_id,
    ROW_NUMBER() OVER (ORDER BY route_order, the_day) AS seq,
    dep_airport, arr_airport, dep_city, arr_city,
    (the_day + dep_hour) AS dep_ts,
    (the_day + dep_hour + flight_duration) AS arr_ts,
    base_price
FROM routes CROSS JOIN days;

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating)
SELECT item_id, 'Volo ' || dep_city || ' - ' || arr_city,
       'Volo di linea ' || dep_city || ' - ' || arr_city || '.',
       base_price, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4
FROM tmp_recurring_flights;

INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, total_seats, stops)
SELECT item_id, dep_airport, arr_airport, dep_city, arr_city, dep_ts, arr_ts, 120, 0
FROM tmp_recurring_flights;

INSERT INTO fare_classes (id, flight_id, name, price, total_seats)
SELECT (SELECT MAX(id) FROM fare_classes) + (seq * 2) - 1, item_id, 'Economy', base_price, 96
FROM tmp_recurring_flights
UNION ALL
SELECT (SELECT MAX(id) FROM fare_classes) + (seq * 2), item_id, 'Business', round(base_price * 2.2, 2), 24
FROM tmp_recurring_flights;

DROP TABLE tmp_recurring_flights;

-- ==========================================
-- SINCRONIZZAZIONE SEQUENZE POSTGRESQL
-- ==========================================
SELECT setval(pg_get_serial_sequence('catalog_items', 'id'), (SELECT MAX(id) FROM catalog_items));
SELECT setval(pg_get_serial_sequence('catalog_images', 'id'), COALESCE((SELECT MAX(id) FROM catalog_images), 1));
SELECT setval(pg_get_serial_sequence('room_types', 'id'), (SELECT MAX(id) FROM room_types));
SELECT setval(pg_get_serial_sequence('fare_classes', 'id'), (SELECT MAX(id) FROM fare_classes));
