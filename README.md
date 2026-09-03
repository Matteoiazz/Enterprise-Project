# Tripify

Piattaforma di prenotazione viaggi (voli, hotel, attività) a **microservizi**: backend Spring Boot + app **Android nativa** (Kotlin, Jetpack Compose).

Il backend gira **tutto in Docker** — 6 microservizi, un database Postgres per servizio, Keycloak, RabbitMQ e il tunnel: non serve installare Java o Maven. L'app Android si compila con Android Studio.

> **Prima di iniziare.** Nel repo c'è il codice, **non le credenziali** (`.env`, `local.properties`, il realm Keycloak e le chiavi del tunnel sono in `.gitignore`, il repo è pubblico). Insieme all'accesso al repo ti abbiamo mandato **a parte** l'archivio **`tripify-config.tgz`**: senza quello lo stack non parte. Come usarlo: [passo 2](#2-scompatta-larchivio-di-configurazione).

---

## Indice

1. [Come è fatto](#come-è-fatto)
2. [Prerequisiti](#prerequisiti)
3. [Avvio](#avvio)
4. [Utenti e ruoli](#utenti-e-ruoli)
5. [Cosa c'è nei file di configurazione](#cosa-cè-nei-file-di-configurazione)
6. [Il tunnel Cloudflare](#il-tunnel-cloudflare)
7. [Porte, console e strumenti](#porte-console-e-strumenti)
8. [Comandi utili](#comandi-utili)
9. [Smoke test: "funziona se…"](#smoke-test-funziona-se)
10. [Se qualcosa non va](#se-qualcosa-non-va)
11. [Note tecniche](#note-tecniche)
12. [Struttura del repository](#struttura-del-repository)

---

## Come è fatto

| Servizio | Porta | Ruolo |
|---|---|---|
| **api-gateway** | 8080 | Unico punto di ingresso: instrada le richieste ai servizi, applica il rate limiting |
| **user-auth-service** | 8081 | Profilo utente, sincronizzazione con Keycloak (nome, ruolo, attributi), reset password, eliminazione account |
| **catalog-service** | 8082 | Catalogo voli/hotel/attività, disponibilità, blocchi temporanei (hold), upload immagini annunci |
| **booking-service** | 8083 | Carrello, checkout, pagamento **simulato**, gestione prenotazioni |
| **communication-service** | 8084 | Chat organizzatore↔viaggiatore, notifiche in tempo reale, recensioni |
| **itinerary-service** | 8085 | Liste di viaggio salvate e condivise |
| **Keycloak** | 8180 | Identity provider OAuth2/OIDC, realm `tripify`, login con Google, tema personalizzato |
| **RabbitMQ** | 5672 / 15672 | Coda di messaggi per le notifiche |
| **Postgres** ×7 | 5433–5438 | Uno schema per servizio (+ uno per Keycloak) |
| **cloudflared** | — | Tunnel Cloudflare stabile: espone `keycloak.tripify.cloud` e `api.tripify.cloud` |

**Autenticazione.** L'utente fa login su Keycloak (anche via Google) e riceve un JWT. Ogni microservizio valida da solo il token: verifica la firma con le chiavi pubbliche di Keycloak (`jwk-set-uri`, presa dalla rete Docker interna) e controlla che il claim `iss` sia `https://keycloak.tripify.cloud/realms/tripify`. Le chiamate fra servizi propagano il token dell'utente.

**Pattern saga.** Quando un checkout fallisce, booking-service deve rilasciare gli "hold" (blocchi di disponibilità) già presi su catalog-service. Questa singola chiamata di compensazione non ha un token utente, quindi usa una **chiave di servizio condivisa** (`INTERNAL_SERVICE_KEY`), identica su booking-service, catalog-service e communication-service.

**App Android.** Parla **solo** con l'api-gateway (`https://api.tripify.cloud`) e con Keycloak (`https://keycloak.tripify.cloud`). Chat e notifiche usano WebSocket/STOMP su `wss://api.tripify.cloud/ws-chat`. Gli URL finiscono in `BuildConfig` a **compile-time**.

---

## Prerequisiti

| Cosa | Note |
|---|---|
| **Docker Desktop** (o Docker Engine + Compose v2) | Assegna alla VM di Docker almeno **8 GB di RAM** e ~10 GB di disco liberi. Verifica: `docker compose version` |
| **Android Studio** (Ladybug o più recente) | Con un **SDK Android** (API 30+) e un emulatore, oppure un telefono Android con debug USB attivo |
| Connessione internet | Al primo avvio scarica immagini Docker e dipendenze Maven/Gradle |

Non serve altro: Java, Maven e i database girano nei container. Android Studio porta con sé il JDK per compilare l'app.

---

## Avvio

### 1. Clona il repository

Sei stato aggiunto come collaboratore: clona con il tuo account GitHub.

```bash
git clone https://github.com/Matteoiazz/Enterprise-Project.git
cd Enterprise-Project
```

### 2. Scompatta l'archivio di configurazione

L'archivio `tripify-config.tgz` che ti abbiamo mandato a parte contiene i file con le credenziali (non sono nel repo). Scompattalo nella radice del progetto:

```bash
tar -xzf /percorso/di/tripify-config.tgz
```

I file finiscono automaticamente al posto giusto:

| File / cartella | Percorso | Cosa contiene |
|---|---|---|
| `.env` | `backend/.env` | Password e chiavi del backend (Keycloak admin, Cloudinary, chiave interna…) |
| cartella `cloudflared/` | `backend/cloudflared/` | `config.yml` + `<UUID>.json`: credenziali del tunnel Cloudflare |
| `realm-export-4.json` | `backend/keycloak-import/realm-export-4.json` | Realm Keycloak `tripify` con SMTP e client secret Google |
| `docker-compose.override.yml` | `backend/docker-compose.override.yml` | Limiti di memoria per le JVM *(serve solo se il backend va in OOM)* |
| `local.properties` | `frontend/local.properties` | Config dell'app Android |

> **Unica cosa da modificare a mano:** in `frontend/local.properties` metti il percorso del **tuo** Android SDK in `sdk.dir`. `BACKEND_IP` e `KEYCLOAK_IP` puntano già ai tunnel, lasciali così.

### 3. Avvia il backend

```bash
cd backend
docker compose up --build -d
```

Il **primo avvio** scarica le immagini, **compila i 6 microservizi** (Maven, dentro i container) e **importa il realm Keycloak**: metti in conto **5–10 minuti**. Gli avvii successivi sono nell'ordine dei secondi grazie alla cache.

Segui l'avanzamento con `docker compose logs -f`. Quando si calma:

```bash
docker compose ps
```

Tutti i servizi devono essere `Up` (i database e alcuni servizi mostrano anche `healthy`). Poi verifica che il tunnel del backend arrivi al gateway:

```bash
curl -si https://api.tripify.cloud/api/v1/catalog/items/search | head -5
```

Deve rispondere con header HTTP e un corpo JSON: significa che tunnel + gateway sono vivi.

### 4. Compila e avvia l'app Android

1. In Android Studio: **Open** → seleziona la cartella `frontend/`.
2. Aspetta il **Gradle sync** (parte da solo se `local.properties` è a posto).
3. Scegli un emulatore o collega un telefono e premi **Run** (▶).

L'app è compilata contro `https://api.tripify.cloud` e `https://keycloak.tripify.cloud`: **funziona da qualsiasi rete**, non serve che telefono e PC siano sulla stessa WiFi.

### 5. Entra

Usa un utente di prova già presente nel realm:

- **Viaggiatore** — email `demo@tripify.it`, password `Demo1234!`
- **Organizzatore** — email `organizer@tripify.it`, password `Demo1234!`

Oppure **registra un nuovo account** o **accedi con Google** (vedi sotto).

---

## Utenti e ruoli

| Utente di prova | Password | Ruolo | Cosa può fare |
|---|---|---|---|
| `demo@tripify.it` | `Demo1234!` | `ROLE_TRAVELER` | Cercare voli/hotel/attività, filtrare, carrello, checkout (pagamento simulato), vedere le proprie prenotazioni, scrivere e votare recensioni, chattare con gli organizzatori, ricevere notifiche, creare e condividere itinerari, gestire profilo / carte / documenti / compagni di viaggio |
| `organizer@tripify.it` | `Demo1234!` | `ROLE_ORGANIZER` | Tutto quello del viaggiatore **+** pubblicare e gestire annunci (voli, hotel, attività), caricarne le foto, disattivarli/riattivarli, vedere le prenotazioni ricevute, rispondere alle recensioni e alle chat |

### Registrazione ed email

La registrazione avviene sulla pagina di Keycloak (link dalla schermata di login dell'app). Nel form si sceglie il **tipo di account**:

- **Viaggiatore** → `ROLE_TRAVELER`
- **Organizzatore** → `ROLE_ORGANIZER` (compila anche i campi azienda: ragione sociale, P. IVA, ecc.)

La **verifica email è attiva**: dopo la registrazione arriva un'email a `tripify.noreply@gmail.com` con un link da cliccare prima di poter accedere.

### Accesso con Google

Il login con Google crea l'account **sempre come `ROLE_TRAVELER`** (la scelta del tipo account non passa dal flusso Google).

Per avere un **organizzatore che usa Google**:

1. registra prima l'account **in modo normale** sulla pagina Keycloak, scegliendo "Organizzatore";
2. poi usa **"Accedi con Google"** con la **stessa email**: Keycloak riconosce l'account già esistente e ci collega l'identità Google, mantenendo il ruolo organizzatore.

Se invece accedi con Google **prima** di aver creato l'account, ottieni un `ROLE_TRAVELER` e non lo si può promuovere a organizzatore dall'app.

---

## Cosa c'è nei file di configurazione

I file dell'archivio sono già pronti: per far girare la demo non serve modificarli (a parte `sdk.dir`). Questa sezione spiega solo cosa contengono.

### `backend/.env`

| Variabile | A cosa serve | Nell'archivio |
|---|---|---|
| `DB_PASSWORD` | Password di tutti i Postgres | valorizzata |
| `KEYCLOAK_ADMIN_USERNAME` / `KEYCLOAK_ADMIN_PASSWORD` | Credenziali admin di Keycloak (console + Admin Client di user-auth-service) | valorizzate |
| `KEYCLOAK_PUBLIC_URL` | `https://keycloak.tripify.cloud` — è l'`iss` atteso nei JWT da app e backend | valorizzata |
| `CLOUDFLARE_TUNNEL_TOKEN` | vuota: il tunnel usa `backend/cloudflared/config.yml` | vuota |
| `LOCAL_IP` | Campo obbligatorio (`docker compose up` non parte se manca). **Sotto Docker il valore non viene usato**, l'`iss` lo impone `KEYCLOAK_PUBLIC_URL` | valorizzata (qualsiasi valore va bene) |
| `INTERNAL_SERVICE_KEY` | Chiave condivisa per la compensazione saga: identica su booking, catalog e communication | valorizzata |
| `RABBITMQ_*` | Credenziali RabbitMQ | default `guest` |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | Upload foto profilo e foto annunci | valorizzate |
| `GATEWAY_RATE_LIMIT_*` | Rate limiting sul gateway | default |

### `frontend/local.properties`

| Voce | Valore | Note |
|---|---|---|
| `sdk.dir` | *(da mettere tu)* | `~/Library/Android/sdk` (macOS), `~/Android/Sdk` (Linux), `C:\Users\<tu>\AppData\Local\Android\Sdk` (Windows) |
| `BACKEND_IP` | `https://api.tripify.cloud` | Se contiene `http` è usato tale e quale; altrimenti diventa `http://<valore>:8080` |
| `KEYCLOAK_IP` | `https://keycloak.tripify.cloud` | Deve combaciare con `KEYCLOAK_PUBLIC_URL` |
| `MAPS_API_KEY` | *(facoltativa)* | Solo per la mini-mappa nel dettaglio di un annuncio |

### `backend/keycloak-import/realm-export-4.json`

Il realm `tripify` completo: client dell'app (`tripify-android-client`, public + PKCE S256, redirect `com.tripify.app://oauth`), provider Google, server SMTP, tema `tripify`, i due utenti di prova, verifica email attiva. Keycloak lo importa **al primo avvio con database vuoto**.

### `backend/docker-compose.override.yml`

Docker Compose lo carica in automatico sopra `docker-compose.yml`. Impone un `-Xmx` a ogni JVM così le 6 insieme non vanno in over-commit. **Serve solo** se `docker compose up` fa crashare la VM di Docker o i servizi vengono uccisi per OOM: se non hai problemi di memoria puoi anche rimuoverlo.

---

## Il tunnel Cloudflare

Keycloak **deve** stare dietro un URL **HTTPS con hostname** (non IP, non `http`): l'IdP Google rifiuta i redirect altrimenti, e il claim `iss` del token deve essere identico per app e backend. Il container `cloudflared`, con le credenziali in `backend/cloudflared/`, apre **un solo tunnel stabile con due hostname**:

- `keycloak.tripify.cloud` → `keycloak:8080`
- `api.tripify.cloud` → `api-gateway:8080`

> Con queste credenziali il tunnel lo può tenere su **una macchina alla volta**. Quando esegui tu `docker compose up`, il tunnel parte dal tuo `cloudflared`: assicurati che nessun altro lo stia già eseguendo.

Se il container `cloudflared` risulta partito ma `api.tripify.cloud` non risponde, ricrealo:

```bash
docker compose up -d --force-recreate cloudflared
```

---

## Porte, console e strumenti

| URL | Cosa |
|---|---|
| `https://api.tripify.cloud` | API pubblica (api-gateway) usata dall'app |
| `https://keycloak.tripify.cloud` | Keycloak pubblico (login, token, redirect Google) |
| `http://localhost:8080` | api-gateway diretto (dalla macchina dove gira Docker) |
| `http://localhost:8180` | Console admin di Keycloak — credenziali = `KEYCLOAK_ADMIN_USERNAME` / `KEYCLOAK_ADMIN_PASSWORD` del `.env` |
| `http://localhost:15672` | UI di RabbitMQ — `guest` / `guest` |
| `localhost:5433…5438` | I singoli Postgres, se vuoi ispezionarli con un client SQL |

---

## Comandi utili

Da dentro `backend/`:

```bash
docker compose ps                          # stato dei servizi
docker compose logs -f                     # log di tutto, in tempo reale
docker compose logs -f booking-service     # log di un servizio
docker compose logs --tail=40 cloudflared  # log del tunnel
docker compose restart catalog-service     # riavvia un servizio
docker compose up -d --build booking-service       # ricompila e riavvia un servizio
docker compose up -d --force-recreate cloudflared  # ricrea il tunnel
docker compose down                        # ferma tutto (dati nei volumi preservati)
docker compose down -v                     # ferma tutto e CANCELLA i dati (DB + realm importato)
```

---

## Smoke test: "funziona se…"

Dopo `docker compose up` e con l'app installata:

1. `docker compose ps` → tutti `Up`.
2. `curl -si https://keycloak.tripify.cloud/realms/tripify` → `200`.
3. `curl -si https://api.tripify.cloud/api/v1/catalog/items/search` → risponde il backend (JSON).
4. App **senza login**: apri il catalogo e la sezione "Organizzatori" → si vedono comunque.
5. App: login con `demo@tripify.it` / `Demo1234!` → arrivi alla home.
6. App: una ricerca voli o hotel → compaiono risultati.
7. App: apri un annuncio, aggiungi al carrello, checkout con una carta valida per formato (16 cifre, scadenza futura) → prenotazione confermata.
8. App: dalla prenotazione apri la chat con l'organizzatore, scrivi un messaggio → con `organizer@tripify.it` lo ricevi in tempo reale e arriva la notifica.
9. App: lascia una recensione sull'annuncio prenotato → compare nel dettaglio.
10. Registra un nuovo utente → arriva l'email di verifica; reset password da login → arriva l'email.

---

## Se qualcosa non va

| Sintomo | Causa probabile | Cosa fare |
|---|---|---|
| Ogni chiamata autenticata torna **401**, lo stack è "verde" | Tunnel Keycloak giù, o `KEYCLOAK_PUBLIC_URL` non combacia con l'`iss` | `docker compose logs cloudflared`; verifica `KEYCLOAK_PUBLIC_URL=https://keycloak.tripify.cloud` in `.env` |
| L'app va in timeout su tutto | Tunnel `api.tripify.cloud` giù, o `BACKEND_IP` sbagliato | `curl -si https://api.tripify.cloud/api/v1/catalog/items/search`; controlla `BACKEND_IP` in `local.properties` e **ricompila** |
| `curl` su `api.tripify.cloud` dà un errore Cloudflare (52x/1016) invece di JSON | Il container `cloudflared` non ha le due regole di ingress | `docker compose up -d --force-recreate cloudflared` |
| `docker compose up` crasha o i servizi muoiono per OOM | Le 6 JVM in over-commit sulla VM di Docker | Assicurati che `backend/docker-compose.override.yml` sia presente, poi `docker compose up -d` |
| `port is already allocated` (8080/8180/5433…) | Un altro processo occupa la porta | Ferma il processo in conflitto o cambia il mapping in `docker-compose.yml` |
| `docker compose up` si rifiuta di partire: errore su `LOCAL_IP` / `INTERNAL_SERVICE_KEY` / `KEYCLOAK_PUBLIC_URL` | Quella variabile è vuota in `.env` | Non dovrebbe capitare con l'archivio; se succede, valorizzala |
| Keycloak riparte in loop al primo avvio | Import del realm fallito | Controlla che `backend/keycloak-import/realm-export-4.json` esista e sia JSON valido; `docker compose down -v` e riprova |
| Login con Google: errore `redirect_uri_mismatch` | Redirect URI non registrato in Google Console | Deve esserci `https://keycloak.tripify.cloud/realms/tripify/broker/google/endpoint` |
| Le email (verifica, reset password) non arrivano | SMTP non raggiungibile | `docker compose logs -f user-auth-service` durante l'invio; controlla che la macchina abbia accesso a `smtp.gmail.com:465` |
| Gradle sync fallisce in Android Studio | `sdk.dir` mancante o sbagliato in `local.properties` | Metti il percorso reale del tuo Android SDK |
| Build dell'app fallisce con `Could not connect to Kotlin daemon` / `Storage ... is already registered` | Cache incrementale Kotlin corrotta | Android Studio chiuso: `cd frontend && ./gradlew --stop && rm -rf app/build/kotlin .gradle/*/kotlin`, poi **Rebuild** |

---

## Note tecniche

- **Compilazione nei container.** Ogni microservizio ha un `Dockerfile` multi-stage (Maven per compilare, JRE leggera per l'esecuzione): `docker compose up --build` compila e avvia tutto il backend in un colpo solo, senza Java/Maven sulla macchina.
- **Config = variabili d'ambiente.** I valori `localhost:...` negli `application.properties` sono solo il fallback per l'esecuzione **fuori** da Docker: dentro Docker li sovrascrive tutti il compose (datasource, URL dei servizi, host RabbitMQ, `issuer-uri`, `jwk-set-uri`).
- **Validazione JWT senza dipendere dal tunnel.** Ogni servizio prende le chiavi pubbliche da `http://keycloak:8080` (rete interna) ma si aspetta `iss = https://keycloak.tripify.cloud/...`: così l'avvio non aspetta il tunnel, ma il token resta quello "pubblico" che usa anche l'app.
- **Pagamento simulato.** Nessun gestore di pagamenti reale: qualunque carta che supera il controllo di formato/Luhn viene approvata.
- **Import del realm una tantum.** Avviene solo al primo avvio con database Keycloak vuoto. Dopo `docker compose down -v` viene rifatto; con un semplice `docker compose down` il realm resta.
- **Primo avvio lento.** Download immagini + compilazione 6 servizi + import realm: diversi minuti. Poi è tutto in cache.

---

## Struttura del repository

```
Enterprise-Project/
├── backend/
│   ├── docker-compose.yml                    # infrastruttura + microservizi
│   ├── docker-compose.override.yml.example   # limiti di memoria JVM (opzionale)
│   ├── .env.example                          # variabili d'ambiente
│   ├── keycloak-import.example.json          # template del realm "tripify" (segreti = CHANGEME)
│   ├── keycloak-import/                      # dove va il realm vero (fuori da Git)
│   ├── keycloak-themes/tripify/              # tema di login ed email personalizzato
│   ├── cloudflared/                          # config del tunnel Cloudflare (fuori da Git)
│   ├── api-gateway/
│   ├── booking-service/
│   ├── catalog-service/
│   ├── communication-service/
│   ├── itinerary-service/
│   └── user-auth-service/
└── frontend/                                 # app Android (Kotlin + Jetpack Compose)
    └── local.properties.example              # config dell'app da copiare in local.properties
```
