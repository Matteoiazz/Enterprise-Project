DELETE FROM list_shares;
DELETE FROM list_items;
DELETE FROM favorite_list_likes;
DELETE FROM favorite_lists;

-- Itinerari pubblici di esempio: ognuno rispetta il requisito minimo (2 voli, 1 hotel,
-- 1 attività) componendo CatalogItem reali già presenti in catalog-service (data.sql).
INSERT INTO favorite_lists (id, name, owner_id, visibility, public_token, city, likes_count, bookings_count, created_at) VALUES
    (1, 'Roma Express', '31c8b93d-d815-49ce-bd59-f22f93d28d12', 'PUBLIC', 'a1b2c3d4-0001-4a11-8e11-000000000001', 'Roma', 24, 6, now()),
    (2, 'Fuga a Venezia', '31c8b93d-d815-49ce-bd59-f22f93d28d12', 'PUBLIC', 'a1b2c3d4-0002-4a11-8e11-000000000002', 'Venezia', 41, 11, now()),
    (3, 'Sicilia On Fire', '31c8b93d-d815-49ce-bd59-f22f93d28d12', 'PUBLIC', 'a1b2c3d4-0003-4a11-8e11-000000000003', 'Catania', 17, 4, now()),
    (4, 'Puglia Autentica', '31c8b93d-d815-49ce-bd59-f22f93d28d12', 'PUBLIC', 'a1b2c3d4-0004-4a11-8e11-000000000004', 'Bari', 33, 9, now()),
    (5, 'Sardegna Blu', '31c8b93d-d815-49ce-bd59-f22f93d28d12', 'PUBLIC', 'a1b2c3d4-0005-4a11-8e11-000000000005', 'Cagliari', 8, 2, now());

-- Roma Express: volo Roma->Milano, volo Roma->Palermo, Hotel Roma (Via del Corso), Tour Colosseo
INSERT INTO list_items (list_id, catalog_item_id) VALUES (1, 1), (1, 3), (1, 16), (1, 33);

-- Fuga a Venezia: volo Venezia->Bari, volo Verona->Trieste, Hotel Canal Grande, Gondola
INSERT INTO list_items (list_id, catalog_item_id) VALUES (2, 5), (2, 12), (2, 19), (2, 34);

-- Sicilia On Fire: volo Torino->Catania, volo Roma->Palermo, Hotel Etna View, Trekking Etna
INSERT INTO list_items (list_id, catalog_item_id) VALUES (3, 4), (3, 3), (3, 22), (3, 38);

-- Puglia Autentica: volo Venezia->Bari, volo Milano->Napoli, Masseria dei Trulli, Tour Alberobello
INSERT INTO list_items (list_id, catalog_item_id) VALUES (4, 5), (4, 2), (4, 23), (4, 39);

-- Sardegna Blu: volo Bologna->Cagliari, volo Napoli->Barcellona, Poetto Beach Hotel, Immersione
INSERT INTO list_items (list_id, catalog_item_id) VALUES (5, 6), (5, 9), (5, 26), (5, 40);

SELECT setval(pg_get_serial_sequence('favorite_lists','id'), (SELECT MAX(id) FROM favorite_lists));
