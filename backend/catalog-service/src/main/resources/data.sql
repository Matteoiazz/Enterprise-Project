DELETE FROM itinerary_items;
DELETE FROM itineraries;
DELETE FROM catalog_images;
DELETE FROM hotel_amenities;
DELETE FROM flight_details;
DELETE FROM hotel_details;
DELETE FROM activity_details;
DELETE FROM catalog_items;

-- ==========================================
-- VOLI (id 1-12)
-- ==========================================

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (1, 'Volo Roma - Milano', 'Volo di linea Roma Fiumicino - Milano Linate, ideale per viaggi di lavoro.', 89.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, available_seats, stops) VALUES
    (1, 'FCO', 'LIN', 'Roma', 'Milano', '2026-09-05 07:30:00', '2026-09-05 08:45:00', 60, 0);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1436491865332-7a61a109cc05?q=80&w=1000&auto=format&fit=crop', 1),
                                                            ('https://images.unsplash.com/photo-1569154941061-e231b4732ef1?q=80&w=1000&auto=format&fit=crop', 1);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (2, 'Volo Milano - Napoli', 'Collegamento diretto tra Malpensa e Capodichino, comodo per il weekend.', 75.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, available_seats, stops) VALUES
    (2, 'MXP', 'NAP', 'Milano', 'Napoli', '2026-08-20 09:15:00', '2026-08-20 10:45:00', 40, 0);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1502920917128-1aa500764cbd?q=80&w=1000&auto=format&fit=crop', 2),
                                                            ('https://images.unsplash.com/photo-1533904828757-e8f28a41d9db?q=80&w=1000&auto=format&fit=crop', 2);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (3, 'Volo Roma - Palermo', 'Volo diretto per la Sicilia, perfetto per esplorare Palermo e dintorni.', 68.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, available_seats, stops) VALUES
    (3, 'FCO', 'PMO', 'Roma', 'Palermo', '2026-07-10 12:00:00', '2026-07-10 13:20:00', 55, 0);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1523592121529-f6dde35f079e?q=80&w=1000&auto=format&fit=crop', 3),
                                                            ('https://images.unsplash.com/photo-1533105079780-92b9be482077?q=80&w=1000&auto=format&fit=crop', 3);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (4, 'Volo Torino - Catania', 'Volo con uno scalo, tariffa economica per raggiungere la Sicilia orientale.', 92.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 3);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, available_seats, stops) VALUES
    (4, 'TRN', 'CTA', 'Torino', 'Catania', '2026-06-18 06:45:00', '2026-06-18 10:30:00', 35, 1);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1523731407965-2430cd12f5e4?q=80&w=1000&auto=format&fit=crop', 4),
                                                            ('https://images.unsplash.com/photo-1591604129939-f1efa4d9f7fa?q=80&w=1000&auto=format&fit=crop', 4);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (5, 'Volo Venezia - Bari', 'Volo diretto dal Nord-Est alla Puglia, ottimo per il mare adriatico.', 79.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, available_seats, stops) VALUES
    (5, 'VCE', 'BRI', 'Venezia', 'Bari', '2026-08-02 15:20:00', '2026-08-02 16:50:00', 48, 0);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1523906834658-6e24ef2386f9?q=80&w=1000&auto=format&fit=crop', 5),
                                                            ('https://images.unsplash.com/photo-1516483638261-f4dbaf036963?q=80&w=1000&auto=format&fit=crop', 5);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (6, 'Volo Bologna - Cagliari', 'Volo diretto per la Sardegna, ideale per le vacanze estive.', 85.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, available_seats, stops) VALUES
    (6, 'BLQ', 'CAG', 'Bologna', 'Cagliari', '2026-07-25 11:10:00', '2026-07-25 12:35:00', 50, 0);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1531572753322-ad063cecc140?q=80&w=1000&auto=format&fit=crop', 6),
                                                            ('https://images.unsplash.com/photo-1519046904884-53103b34b206?q=80&w=1000&auto=format&fit=crop', 6);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (7, 'Volo Roma - Londra', 'Volo di linea. Perfetto per raggiungere la conferenza internazionale di medicina d''urgenza.', 180.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 5);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, available_seats, stops) VALUES
    (7, 'FCO', 'LHR', 'Roma', 'Londra', '2026-09-10 08:00:00', '2026-09-10 10:30:00', 30, 0);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1513628253939-010e64ac66cd?q=80&w=1000&auto=format&fit=crop', 7),
                                                            ('https://images.unsplash.com/photo-1520106212299-d99c443e4568?q=80&w=1000&auto=format&fit=crop', 7);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (8, 'Volo Milano - Parigi', 'Volo diretto Malpensa - Charles de Gaulle, più frequenze giornaliere.', 145.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, available_seats, stops) VALUES
    (8, 'MXP', 'CDG', 'Milano', 'Parigi', '2026-10-01 07:00:00', '2026-10-01 08:40:00', 42, 0);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1502602898657-3e91760cbb34?q=80&w=1000&auto=format&fit=crop', 8),
                                                            ('https://images.unsplash.com/photo-1499856871958-5b9627545d1a?q=80&w=1000&auto=format&fit=crop', 8);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (9, 'Volo Napoli - Barcellona', 'Volo diretto verso la Catalogna, tariffa low-cost.', 99.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 3);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, available_seats, stops) VALUES
    (9, 'NAP', 'BCN', 'Napoli', 'Barcellona', '2026-06-14 13:45:00', '2026-06-14 15:40:00', 38, 0);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1523531294919-4bcd7c65e216?q=80&w=1000&auto=format&fit=crop', 9),
                                                            ('https://images.unsplash.com/photo-1583422409516-2895a77efded?q=80&w=1000&auto=format&fit=crop', 9);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (10, 'Volo Roma - New York', 'Volo diretto operato da ITA Airways. Include bagaglio in stiva.', 550.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 4);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, available_seats, stops) VALUES
    (10, 'FCO', 'JFK', 'Roma', 'New York', '2026-06-01 10:00:00', '2026-06-01 14:00:00', 120, 0);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1499591934245-40b55745b905?q=80&w=1000&auto=format&fit=crop', 10),
                                                            ('https://images.unsplash.com/photo-1542296332-2e4473faf563?q=80&w=1000&auto=format&fit=crop', 10);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (11, 'Volo Milano - Lamezia Terme', 'Volo low cost per scendere giù al sud. Solo bagaglio a mano.', 45.99, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 3);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, available_seats, stops) VALUES
    (11, 'MXP', 'SUF', 'Milano', 'Lamezia Terme', '2026-07-15 18:30:00', '2026-07-15 20:15:00', 45, 0);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1530521954074-e64f6810b32d?q=80&w=1000&auto=format&fit=crop', 11),
                                                            ('https://images.unsplash.com/photo-1506012787146-f92b2d7d6d96?q=80&w=1000&auto=format&fit=crop', 11);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (12, 'Volo Verona - Trieste', 'Volo regionale, comodo per collegare Nord-Est e Nord-Ovest.', 59.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Voli', 3);
INSERT INTO flight_details (id, departure_airport, arrival_airport, departure_city, arrival_city, departure_time, arrival_time, available_seats, stops) VALUES
    (12, 'VRN', 'TRS', 'Verona', 'Trieste', '2026-05-20 09:00:00', '2026-05-20 09:55:00', 28, 0);
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1595425964272-5b6a0cb6b6e3?q=80&w=1000&auto=format&fit=crop', 12),
                                                            ('https://images.unsplash.com/photo-1591810521626-9d3877dd0e0b?q=80&w=1000&auto=format&fit=crop', 12);

-- ==========================================
-- HOTEL (id 13-30)
-- ==========================================

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (13, 'Hotel Hilton Times Square', 'Soggiorno di lusso nel cuore di Manhattan con vista panoramica.', 250.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 5);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city) VALUES
    (13, 40.7589, -73.9851, 'Double Deluxe', 15, '234 W 42nd St, New York, NY 10036', 'New York');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (13, 'Wi-Fi'), (13, 'Palestra'), (13, 'Room Service'), (13, 'Aria Condizionata');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1566073771259-6a8506099945?q=80&w=1000&auto=format&fit=crop', 13),
                                                            ('https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?q=80&w=1000&auto=format&fit=crop', 13),
                                                            ('https://images.unsplash.com/photo-1582719478250-c89402bb6539?q=80&w=1000&auto=format&fit=crop', 13);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (14, 'Campus Relax Hotel', 'Struttura moderna a due passi dall''Università. Wi-Fi veloce e area studio.', 65.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city) VALUES
    (14, 39.3615, 16.2285, 'Camera Singola Studenti', 8, 'Via Pietro Bucci, 87036 Rende (CS)', 'Rende');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (14, 'Wi-Fi'), (14, 'Area Studio'), (14, 'Parcheggio');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1555854877-bab0e564b8d5?q=80&w=1000&auto=format&fit=crop', 14),
                                                            ('https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?q=80&w=1000&auto=format&fit=crop', 14),
                                                            ('https://images.unsplash.com/photo-1497366216548-37526070297c?q=80&w=1000&auto=format&fit=crop', 14);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (15, 'Iron & Spa Resort', 'Hotel con palestra attrezzatissima per powerlifting, rack professionali e area benessere.', 120.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 5);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city) VALUES
    (15, 45.4642, 9.1900, 'Suite con Pesi Liberi', 3, 'Via Roma 10, 20121 Milano', 'Milano');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (15, 'Wi-Fi'), (15, 'Palestra'), (15, 'Spa'), (15, 'Piscina');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1534438327276-14e5300c3a48?q=80&w=1000&auto=format&fit=crop', 15),
                                                            ('https://images.unsplash.com/photo-1571902943202-507ec2618e8f?q=80&w=1000&auto=format&fit=crop', 15),
                                                            ('https://images.unsplash.com/photo-1540497077202-7c8a3999166f?q=80&w=1000&auto=format&fit=crop', 15);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (16, 'Art & Ink Boutique Hotel', 'Struttura dal design post-industriale. Al piano terra si trova uno studio di tatuatori residenti.', 95.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city) VALUES
    (16, 41.9028, 12.4964, 'Loft Industriale', 5, 'Via del Corso 45, 00186 Roma', 'Roma');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (16, 'Wi-Fi'), (16, 'Bar'), (16, 'Parcheggio');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1560066984-138dadb4c035?q=80&w=1000&auto=format&fit=crop', 16),
                                                            ('https://images.unsplash.com/photo-1598331668904-45ea0f3b4d45?q=80&w=1000&auto=format&fit=crop', 16),
                                                            ('https://images.unsplash.com/photo-1512406830500-1c05000570fc?q=80&w=1000&auto=format&fit=crop', 16);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (17, 'Gamer''s Haven Lodge', 'Hotel dedicato agli eSports. Connessione fibra dedicata, postazioni PC in camera, ideale per raid notturni.', 110.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city) VALUES
    (17, 52.5200, 13.4050, 'Gaming Suite 2 Postazioni', 12, 'Alexanderplatz 5, 10178 Berlin', 'Berlino');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (17, 'Wi-Fi'), (17, 'Fibra Dedicata'), (17, 'Aria Condizionata');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1542751371-adc38448a05e?q=80&w=1000&auto=format&fit=crop', 17),
                                                            ('https://images.unsplash.com/photo-1593305841991-05c297ba4575?q=80&w=1000&auto=format&fit=crop', 17),
                                                            ('https://images.unsplash.com/photo-1538481199705-c710c4e965fc?q=80&w=1000&auto=format&fit=crop', 17);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (18, 'Palazzo Vecchio Suites', 'Dimora storica restaurata nel centro di Firenze, a due passi dagli Uffizi.', 210.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 5);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city) VALUES
    (18, 43.7696, 11.2558, 'Suite Rinascimentale', 6, 'Via dei Calzaiuoli 12, 50122 Firenze', 'Firenze');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (18, 'Wi-Fi'), (18, 'Colazione Inclusa'), (18, 'Room Service'), (18, 'Aria Condizionata');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1541971875076-8f970d573be6?q=80&w=1000&auto=format&fit=crop', 18),
                                                            ('https://images.unsplash.com/photo-1445019980597-93fa8acb246c?q=80&w=1000&auto=format&fit=crop', 18),
                                                            ('https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?q=80&w=1000&auto=format&fit=crop', 18);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (19, 'Canal Grande Boutique Hotel', 'Vista diretta sul Canal Grande, arredi veneziani originali del XVIII secolo.', 280.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 5);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city) VALUES
    (19, 45.4408, 12.3155, 'Camera Vista Canale', 4, 'Fondamenta del Vin 34, 30124 Venezia', 'Venezia');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (19, 'Wi-Fi'), (19, 'Colazione Inclusa'), (19, 'Bar');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1523906834658-6e24ef2386f9?q=80&w=1000&auto=format&fit=crop', 19),
                                                            ('https://images.unsplash.com/photo-1534113414509-0eec2bfb493f?q=80&w=1000&auto=format&fit=crop', 19),
                                                            ('https://images.unsplash.com/photo-1514890547357-a9ee288728e0?q=80&w=1000&auto=format&fit=crop', 19);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (20, 'Portici Rossi Hotel', 'Hotel elegante sotto i portici del centro storico di Bologna, vicino alle Due Torri.', 130.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city) VALUES
    (20, 44.4949, 11.3426, 'Doppia Classic', 10, 'Via Rizzoli 8, 40125 Bologna', 'Bologna');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (20, 'Wi-Fi'), (20, 'Colazione Inclusa'), (20, 'Parcheggio');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1555400038-63f5ba517a47?q=80&w=1000&auto=format&fit=crop', 20),
                                                            ('https://images.unsplash.com/photo-1445019980597-93fa8acb246c?q=80&w=1000&auto=format&fit=crop', 20),
                                                            ('https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?q=80&w=1000&auto=format&fit=crop', 20);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (21, 'Mondello Beach Resort', 'Resort fronte mare a Mondello, con spiaggia privata e piscina panoramica.', 165.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 5);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city) VALUES
    (21, 38.1938, 13.3266, 'Camera Vista Mare', 20, 'Viale Regina Elena 45, 90151 Palermo', 'Palermo');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (21, 'Wi-Fi'), (21, 'Piscina'), (21, 'Spiaggia Privata'), (21, 'Colazione Inclusa');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?q=80&w=1000&auto=format&fit=crop', 21),
                                                            ('https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?q=80&w=1000&auto=format&fit=crop', 21),
                                                            ('https://images.unsplash.com/photo-1519821172141-b5d8342c2a24?q=80&w=1000&auto=format&fit=crop', 21);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (22, 'Etna View Country House', 'Agriturismo panoramico alle pendici dell''Etna, immerso tra vigneti e uliveti.', 88.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city) VALUES
    (22, 37.6100, 15.1500, 'Camera Country', 7, 'Contrada Etna 3, 95030 Catania', 'Catania');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (22, 'Wi-Fi'), (22, 'Colazione Inclusa'), (22, 'Parcheggio'), (22, 'Piscina');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1502301103665-0b95cc738daf?q=80&w=1000&auto=format&fit=crop', 22),
                                                            ('https://images.unsplash.com/photo-1568605114967-8130f3a36994?q=80&w=1000&auto=format&fit=crop', 22),
                                                            ('https://images.unsplash.com/photo-1518733057094-95b53143d2a7?q=80&w=1000&auto=format&fit=crop', 22);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (23, 'Masseria dei Trulli', 'Masseria tradizionale pugliese convertita in hotel diffuso, vicino ad Alberobello.', 140.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 5);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city) VALUES
    (23, 41.1177, 16.8719, 'Trullo Deluxe', 9, 'Contrada Trulli 21, 70011 Alberobello (BA)', 'Bari');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (23, 'Wi-Fi'), (23, 'Piscina'), (23, 'Colazione Inclusa'), (23, 'Parcheggio');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1523731407965-2430cd12f5e4?q=80&w=1000&auto=format&fit=crop', 23),
                                                            ('https://images.unsplash.com/photo-1587213811864-73fdb3617d7c?q=80&w=1000&auto=format&fit=crop', 23),
                                                            ('https://images.unsplash.com/photo-1595877244574-e90ce41ce089?q=80&w=1000&auto=format&fit=crop', 23);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (24, 'Porto Antico Hotel', 'Hotel moderno affacciato sul Porto Antico di Genova, a due passi dall''Acquario.', 105.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city) VALUES
    (24, 44.4056, 8.9463, 'Doppia Vista Porto', 11, 'Calata Cattaneo 2, 16126 Genova', 'Genova');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (24, 'Wi-Fi'), (24, 'Room Service'), (24, 'Parcheggio');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?q=80&w=1000&auto=format&fit=crop', 24),
                                                            ('https://images.unsplash.com/photo-1455587734955-081b22074882?q=80&w=1000&auto=format&fit=crop', 24),
                                                            ('https://images.unsplash.com/photo-1571896349842-33c89424de2d?q=80&w=1000&auto=format&fit=crop', 24);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (25, 'Verona Romantica Hotel', 'Hotel boutique a pochi passi da Casa di Giulietta, arredamento romantico.', 115.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city) VALUES
    (25, 45.4384, 10.9916, 'Camera Romeo e Giulietta', 8, 'Via Cappello 20, 37121 Verona', 'Verona');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (25, 'Wi-Fi'), (25, 'Colazione Inclusa'), (25, 'Bar');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1555400038-63f5ba517a47?q=80&w=1000&auto=format&fit=crop', 25),
                                                            ('https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?q=80&w=1000&auto=format&fit=crop', 25),
                                                            ('https://images.unsplash.com/photo-1445019980597-93fa8acb246c?q=80&w=1000&auto=format&fit=crop', 25);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (26, 'Poetto Beach Hotel', 'Hotel a due passi dalla spiaggia del Poetto, con terrazza panoramica sul golfo.', 98.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city) VALUES
    (26, 39.2103, 9.1547, 'Camera Vista Golfo', 14, 'Viale Poetto 100, 09126 Cagliari', 'Cagliari');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (26, 'Wi-Fi'), (26, 'Colazione Inclusa'), (26, 'Parcheggio'), (26, 'Bar');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1519046904884-53103b34b206?q=80&w=1000&auto=format&fit=crop', 26),
                                                            ('https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?q=80&w=1000&auto=format&fit=crop', 26),
                                                            ('https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?q=80&w=1000&auto=format&fit=crop', 26);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (27, 'Barocco Salentino Hotel', 'Palazzo barocco restaurato nel centro storico di Lecce, la "Firenze del Sud".', 125.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 5);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city) VALUES
    (27, 40.3515, 18.1750, 'Suite Barocca', 6, 'Via Palmieri 15, 73100 Lecce', 'Lecce');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (27, 'Wi-Fi'), (27, 'Colazione Inclusa'), (27, 'Room Service');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1541971875076-8f970d573be6?q=80&w=1000&auto=format&fit=crop', 27),
                                                            ('https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?q=80&w=1000&auto=format&fit=crop', 27),
                                                            ('https://images.unsplash.com/photo-1587213811864-73fdb3617d7c?q=80&w=1000&auto=format&fit=crop', 27);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (28, 'Grand Hotel Rimini Mare', 'Hotel storico fronte mare a Rimini, con stabilimento balneare privato.', 135.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city) VALUES
    (28, 44.0678, 12.5695, 'Camera Vista Mare', 25, 'Parco Federico Fellini 1, 47921 Rimini', 'Rimini');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (28, 'Wi-Fi'), (28, 'Piscina'), (28, 'Spiaggia Privata'), (28, 'Colazione Inclusa');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1519046904884-53103b34b206?q=80&w=1000&auto=format&fit=crop', 28),
                                                            ('https://images.unsplash.com/photo-1519821172141-b5d8342c2a24?q=80&w=1000&auto=format&fit=crop', 28),
                                                            ('https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?q=80&w=1000&auto=format&fit=crop', 28);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (29, 'Torre Pendente Hotel', 'Hotel a 5 minuti a piedi dalla Torre di Pisa, con vista su Piazza dei Miracoli.', 112.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 4);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city) VALUES
    (29, 43.7228, 10.3966, 'Camera Vista Torre', 9, 'Via Santa Maria 55, 56126 Pisa', 'Pisa');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (29, 'Wi-Fi'), (29, 'Colazione Inclusa'), (29, 'Bar');
INSERT INTO catalog_images (image_url, catalog_item_id) VALUES
                                                            ('https://images.unsplash.com/photo-1543429257-3e7c2c6d4d9b?q=80&w=1000&auto=format&fit=crop', 29),
                                                            ('https://images.unsplash.com/photo-1541971875076-8f970d573be6?q=80&w=1000&auto=format&fit=crop', 29),
                                                            ('https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?q=80&w=1000&auto=format&fit=crop', 29);

INSERT INTO catalog_items (id, title, description, price, currency, host_id, is_active, category, rating) VALUES
    (30, 'Contrada Palio Hotel', 'Hotel nel cuore di Siena, sulla storica Piazza del Campo, patria del Palio.', 128.00, 'EUR', '31c8b93d-d815-49ce-bd59-f22f93d28d12', true, 'Hotel', 5);
INSERT INTO hotel_details (id, location_lat, location_lng, room_type, available_rooms, address, city) VALUES
    (30, 43.3188, 11.3308, 'Camera Vista Piazza', 7, 'Piazza del Campo 30, 53100 Siena', 'Siena');
INSERT INTO hotel_amenities (hotel_id, amenity) VALUES (30, 'Wi-Fi'), (30, 'Colazione Inclusa'), (30, 'Room Service');
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
-- SINCRONIZZAZIONE SEQUENZE POSTGRESQL
-- ==========================================
SELECT setval(pg_get_serial_sequence('catalog_items', 'id'), (SELECT MAX(id) FROM catalog_items));
SELECT setval(pg_get_serial_sequence('catalog_images', 'id'), COALESCE((SELECT MAX(id) FROM catalog_images), 1));