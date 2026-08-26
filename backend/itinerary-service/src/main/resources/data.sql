-- Questo script gira ad OGNI avvio (spring.sql.init.mode=always), per tenere le 5
-- liste demo sempre coerenti con le rotte/hotel di catalog-service. Le DELETE sono
-- perciò scoped ai soli id 1-5 (le liste demo): una DELETE FROM favorite_lists senza
-- filtro cancellerebbe anche gli itinerari reali creati dagli utenti ad ogni riavvio.
DELETE FROM list_shares WHERE list_id IN (1, 2, 3, 4, 5);
DELETE FROM list_items WHERE list_id IN (1, 2, 3, 4, 5);
DELETE FROM favorite_list_likes WHERE list_id IN (1, 2, 3, 4, 5);
DELETE FROM favorite_lists WHERE id IN (1, 2, 3, 4, 5);

-- Itinerari pubblici di esempio: ognuno rispetta il requisito minimo (2 voli, 1 hotel,
-- 1 attività) componendo CatalogItem reali già presenti in catalog-service (data.sql).
INSERT INTO favorite_lists (id, name, owner_id, visibility, public_token, city, likes_count, bookings_count, created_at) VALUES
    (1, 'Roma Express', '31c8b93d-d815-49ce-bd59-f22f93d28d12', 'PUBLIC', 'a1b2c3d4-0001-4a11-8e11-000000000001', 'Roma', 24, 6, now()),
    (2, 'Fuga a Venezia', '31c8b93d-d815-49ce-bd59-f22f93d28d12', 'PUBLIC', 'a1b2c3d4-0002-4a11-8e11-000000000002', 'Venezia', 41, 11, now()),
    (3, 'Sicilia On Fire', '31c8b93d-d815-49ce-bd59-f22f93d28d12', 'PUBLIC', 'a1b2c3d4-0003-4a11-8e11-000000000003', 'Catania', 17, 4, now()),
    (4, 'Puglia Autentica', '31c8b93d-d815-49ce-bd59-f22f93d28d12', 'PUBLIC', 'a1b2c3d4-0004-4a11-8e11-000000000004', 'Bari', 33, 9, now()),
    (5, 'Sardegna Blu', '31c8b93d-d815-49ce-bd59-f22f93d28d12', 'PUBLIC', 'a1b2c3d4-0005-4a11-8e11-000000000005', 'Cagliari', 8, 2, now());

-- Ogni itinerario qui sotto è una vera andata/ritorno verso una singola città, con
-- l'hotel e l'attività nella STESSA città in cui atterra il volo di andata, e con le
-- date dell'hotel/attività dentro la finestra [arrivo andata, partenza ritorno] —
-- esattamente le regole che ItineraryService.addItemToList impone su ogni nuova
-- aggiunta (vedi validateItineraryCoherence). I voli usati sono le coppie andata/
-- ritorno dedicate in catalog-service/data.sql (id 44-53): i voli one-off 1-12 sono
-- tutti a senso unico e non potrebbero mai formare un itinerario coerente.
-- item_order è obbligatorio: la collection "items" di FavoriteList usa @OrderColumn
-- (l'ordine è semanticamente l'ordine cronologico del viaggio), quindi ogni riga deve
-- avere un indice 0-based esplicito per lista, altrimenti Hibernate non riesce a
-- ricostruire la lista ordinata (item_order NULL) e la lettura fallisce con un 500.

-- Roma Express: volo Milano->Roma, Hotel Roma (Art & Ink), Tour Colosseo, volo Roma->Milano
INSERT INTO list_items (list_id, catalog_item_id, quantity, room_type_id, fare_class_id, check_in, check_out, activity_date, item_order) VALUES
    (1, 44, 1, NULL, 25, NULL, NULL, NULL, 0),
    (1, 16, 1, 8, NULL, '2026-09-03', '2026-09-06', NULL, 1),
    (1, 33, 1, NULL, NULL, NULL, NULL, '2026-09-04', 2),
    (1, 45, 1, NULL, 27, NULL, NULL, NULL, 3);

-- Fuga a Venezia: volo Roma->Venezia, Hotel Canal Grande, Gondola, volo Venezia->Roma
INSERT INTO list_items (list_id, catalog_item_id, quantity, room_type_id, fare_class_id, check_in, check_out, activity_date, item_order) VALUES
    (2, 46, 1, NULL, 29, NULL, NULL, NULL, 0),
    (2, 19, 1, 14, NULL, '2026-09-10', '2026-09-13', NULL, 1),
    (2, 34, 1, NULL, NULL, NULL, NULL, '2026-09-11', 2),
    (2, 47, 1, NULL, 31, NULL, NULL, NULL, 3);

-- Sicilia On Fire: volo Roma->Catania, Hotel Etna View, Trekking Etna, volo Catania->Roma
INSERT INTO list_items (list_id, catalog_item_id, quantity, room_type_id, fare_class_id, check_in, check_out, activity_date, item_order) VALUES
    (3, 48, 1, NULL, 33, NULL, NULL, NULL, 0),
    (3, 22, 1, 21, NULL, '2026-09-17', '2026-09-21', NULL, 1),
    (3, 38, 1, NULL, NULL, NULL, NULL, '2026-09-19', 2),
    (3, 49, 1, NULL, 35, NULL, NULL, NULL, 3);

-- Puglia Autentica: volo Roma->Bari, Masseria dei Trulli, Tour Bari Vecchia, volo Bari->Roma
INSERT INTO list_items (list_id, catalog_item_id, quantity, room_type_id, fare_class_id, check_in, check_out, activity_date, item_order) VALUES
    (4, 50, 1, NULL, 37, NULL, NULL, NULL, 0),
    (4, 23, 1, 23, NULL, '2026-09-24', '2026-09-27', NULL, 1),
    (4, 54, 1, NULL, NULL, NULL, NULL, '2026-09-25', 2),
    (4, 51, 1, NULL, 39, NULL, NULL, NULL, 3);

-- Sardegna Blu: volo Roma->Cagliari, Poetto Beach Hotel, Immersione, volo Cagliari->Roma
INSERT INTO list_items (list_id, catalog_item_id, quantity, room_type_id, fare_class_id, check_in, check_out, activity_date, item_order) VALUES
    (5, 52, 1, NULL, 41, NULL, NULL, NULL, 0),
    (5, 26, 1, 29, NULL, '2026-10-01', '2026-10-05', NULL, 1),
    (5, 40, 1, NULL, NULL, NULL, NULL, '2026-10-03', 2),
    (5, 53, 1, NULL, 43, NULL, NULL, NULL, 3);

SELECT setval(pg_get_serial_sequence('favorite_lists','id'), (SELECT MAX(id) FROM favorite_lists));
