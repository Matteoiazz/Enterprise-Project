DELETE FROM itinerary_items;
DELETE FROM itineraries;
DELETE FROM catalog_images;
DELETE FROM hotel_amenities;
DELETE FROM flight_details;
DELETE FROM hotel_details;
DELETE FROM activity_details;
DELETE FROM catalog_items;

-- 1. VOLO Roma - New York
INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating)
VALUES (1, 'Volo Roma - New York', 'Volo diretto operato da ITA Airways. Include bagaglio in stiva.', 550.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, available_seats, stops)
VALUES (1, 'FCO', 'JFK', 'Roma', 'New York', '2026-06-01 10:00:00', '2026-06-01 14:00:00', 120, 0);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1436491865332-7a61a109cc05?q=80&w=1000&auto=format&fit=crop', 1),
                                                            ('https://images.unsplash.com/photo-1542296332-2e4473faf563?q=80&w=1000&auto=format&fit=crop', 1),
                                                            ('https://images.unsplash.com/photo-1499591934245-40b55745b905?q=80&w=1000&auto=format&fit=crop', 1),
                                                            ('https://images.unsplash.com/photo-1569154941061-e231b4732ef1?q=80&w=1000&auto=format&fit=crop', 1);

-- 2. HOTEL New York
INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating)
VALUES (2, 'Hotel Hilton Times Square', 'Soggiorno di lusso nel cuore di Manhattan con vista panoramica.', 250.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 5);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city)
VALUES (2, 40.7589, -73.9851, 'Double Deluxe', 15, '234 W 42nd St, New York, NY 10036', 'New York');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES
                                                    (2, 'Wi-Fi'), (2, 'Palestra'), (2, 'Room Service'), (2, 'Aria Condizionata');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1566073771259-6a8506099945?q=80&w=1000&auto=format&fit=crop', 2),
                                                            ('https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?q=80&w=1000&auto=format&fit=crop', 2),
                                                            ('https://images.unsplash.com/photo-1582719478250-c89402bb6539?q=80&w=1000&auto=format&fit=crop', 2),
                                                            ('https://images.unsplash.com/photo-1590490359683-658d3d23f972?q=80&w=1000&auto=format&fit=crop', 2);

-- 3. VOLO Milano - Lamezia
INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating)
VALUES (3, 'Volo Milano - Lamezia Terme', 'Volo low cost per scendere giù al sud. Solo bagaglio a mano.', 45.99, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 3);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, available_seats, stops)
VALUES (3, 'MXP', 'SUF', 'Milano', 'Lamezia Terme', '2026-07-15 18:30:00', '2026-07-15 20:15:00', 45, 0);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1530521954074-e64f6810b32d?q=80&w=1000&auto=format&fit=crop', 3),
                                                            ('https://images.unsplash.com/photo-1506012787146-f92b2d7d6d96?q=80&w=1000&auto=format&fit=crop', 3),
                                                            ('https://images.unsplash.com/photo-1464037866556-6812c9d1c72e?q=80&w=1000&auto=format&fit=crop', 3);

-- 4. HOTEL Rende (Campus)
INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating)
VALUES (4, 'Campus Relax Hotel', 'Struttura moderna a due passi dall''Università. Wi-Fi veloce e area studio.', 65.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city)
VALUES (4, 39.3615, 16.2285, 'Camera Singola Studenti', 8, 'Via Pietro Bucci, 87036 Rende (CS)', 'Rende');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES
                                                    (4, 'Wi-Fi'), (4, 'Area Studio'), (4, 'Parcheggio');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1555854877-bab0e564b8d5?q=80&w=1000&auto=format&fit=crop', 4),
                                                            ('https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?q=80&w=1000&auto=format&fit=crop', 4),
                                                            ('https://images.unsplash.com/photo-1497366216548-37526070297c?q=80&w=1000&auto=format&fit=crop', 4),
                                                            ('https://images.unsplash.com/photo-1524813686514-a57563d77965?q=80&w=1000&auto=format&fit=crop', 4);

-- 5. HOTEL Fitness Resort (Milano)
INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating)
VALUES (5, 'Iron & Spa Resort', 'Hotel con palestra attrezzatissima per powerlifting, rack professionali e area benessere.', 120.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 5);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city)
VALUES (5, 45.4642, 9.1900, 'Suite con Pesi Liberi', 3, 'Via Roma 10, 20121 Milano', 'Milano');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES
                                                    (5, 'Wi-Fi'), (5, 'Palestra'), (5, 'Spa'), (5, 'Piscina');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1534438327276-14e5300c3a48?q=80&w=1000&auto=format&fit=crop', 5),
                                                            ('https://images.unsplash.com/photo-1571902943202-507ec2618e8f?q=80&w=1000&auto=format&fit=crop', 5),
                                                            ('https://images.unsplash.com/photo-1540497077202-7c8a3999166f?q=80&w=1000&auto=format&fit=crop', 5),
                                                            ('https://images.unsplash.com/photo-1558611848-73f7eb4001a1?q=80&w=1000&auto=format&fit=crop', 5);

-- 6. HOTEL Tattoo Boutique (Roma)
INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating)
VALUES (6, 'Art & Ink Boutique Hotel', 'Struttura dal design post-industriale. Al piano terra si trova uno studio di tatuatori residenti.', 95.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city)
VALUES (6, 41.9028, 12.4964, 'Loft Industriale', 5, 'Via del Corso 45, 00186 Roma', 'Roma');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES
                                                    (6, 'Wi-Fi'), (6, 'Bar'), (6, 'Parcheggio');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1560066984-138dadb4c035?q=80&w=1000&auto=format&fit=crop', 6),
                                                            ('https://images.unsplash.com/photo-1598331668904-45ea0f3b4d45?q=80&w=1000&auto=format&fit=crop', 6),
                                                            ('https://images.unsplash.com/photo-1512406830500-1c05000570fc?q=80&w=1000&auto=format&fit=crop', 6);

-- 7. VOLO Roma - Londra
INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating)
VALUES (7, 'Volo Roma - Londra', 'Volo di linea. Perfetto per raggiungere la conferenza internazionale di medicina d''urgenza.', 180.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 5);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, available_seats, stops)
VALUES (7, 'FCO', 'LHR', 'Roma', 'Londra', '2026-09-10 08:00:00', '2026-09-10 10:30:00', 30, 0);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1513628253939-010e64ac66cd?q=80&w=1000&auto=format&fit=crop', 7),
                                                            ('https://images.unsplash.com/photo-1476973216892-dbab152f200c?q=80&w=1000&auto=format&fit=crop', 7),
                                                            ('https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=1000&auto=format&fit=crop', 7),
                                                            ('https://images.unsplash.com/photo-1503614472-8c93d56e92ce?q=80&w=1000&auto=format&fit=crop', 7),
                                                            ('https://images.unsplash.com/photo-1520106212299-d99c443e4568?q=80&w=1000&auto=format&fit=crop', 7);

-- 8. HOTEL LAN Party (Berlino)
INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating)
VALUES (8, 'Gamer''s Haven Lodge', 'Hotel dedicato agli eSports. Connessione fibra dedicata, postazioni PC in camera, ideale per raid notturni.', 110.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city)
VALUES (8, 52.5200, 13.4050, 'Gaming Suite 2 Postazioni', 12, 'Alexanderplatz 5, 10178 Berlin', 'Berlino');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES
                                                    (8, 'Wi-Fi'), (8, 'Fibra Dedicata'), (8, 'Aria Condizionata');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1542751371-adc38448a05e?q=80&w=1000&auto=format&fit=crop', 8),
                                                            ('https://images.unsplash.com/photo-1593305841991-05c297ba4575?q=80&w=1000&auto=format&fit=crop', 8),
                                                            ('https://images.unsplash.com/photo-1538481199705-c710c4e965fc?q=80&w=1000&auto=format&fit=crop', 8),
                                                            ('https://images.unsplash.com/photo-1493711662062-fa541adb3fc8?q=80&w=1000&auto=format&fit=crop', 8);

-- 9. ATTIVITÀ Trekking Sila
INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating)
VALUES (9, 'Trekking Impegnativo in Sila', 'Percorso avanzato nei boschi silani. Ottimo per testare la propria resistenza e staccare la spina dallo schermo.', 45.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Attività', 5);
INSERT INTO activity_details (id, activity_type, duration, meeting_point, city, max_participants, guide_included)
VALUES (9, 'Sport e Natura', '6 ore', 'Centro Visite Cupone, Parco Nazionale della Sila', 'Sila', 15, true);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1551632811-561f32228f3c?q=80&w=1000&auto=format&fit=crop', 9),
                                                            ('https://images.unsplash.com/photo-1470071131384-001b85755536?q=80&w=1000&auto=format&fit=crop', 9),
                                                            ('https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=1000&auto=format&fit=crop', 9);

-- 10. ATTIVITÀ Evento Fantasy
INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating)
VALUES (10, 'Evento Fantasy: Difesa del Castello', 'Giornata immersiva di gioco di ruolo dal vivo (LARP) in un borgo medievale. Scegli la tua fazione, farma risorse e difendi l''avamposto!', 30.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Attività', 4);
INSERT INTO activity_details (id, activity_type, duration, meeting_point, city, max_participants, guide_included)
VALUES (10, 'Evento dal Vivo', 'Giornata intera', 'Castello Svevo, Rocca Imperiale', 'Rocca Imperiale', 50, false);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1605806616949-1e87b487cb2a?q=80&w=1000&auto=format&fit=crop', 10),
                                                            ('https://images.unsplash.com/photo-1590055531615-f16d36ffe8ea?q=80&w=1000&auto=format&fit=crop', 10);

SELECT setval(pg_get_serial_sequence('catalog_items', 'id'), (SELECT MAX(id) FROM catalog_items));
SELECT setval(pg_get_serial_sequence('catalog_images', 'id'), COALESCE((SELECT MAX(id) FROM catalog_images), 1));