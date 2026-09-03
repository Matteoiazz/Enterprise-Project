# Tripify

Piattaforma di prenotazione viaggi (voli, hotel, attività) a microservizi: backend Spring Boot + app Android nativa (Kotlin, Jetpack Compose).

Il backend gira **tutto in Docker** (microservizi, database, Keycloak, RabbitMQ): non serve installare Java o Maven. L'app Android si compila con Android Studio.

---

## Indice

- [Avvio rapido (per il docente)](#avvio-rapido-per-il-docente)
- [Utenti di prova](#utenti-di-prova)
- [Architettura](#architettura)
- [Struttura del repository](#struttura-del-repository)
- [Configurazione dettagliata](#configurazione-dettagliata)
- [Il tunnel Cloudflare](#il-tunnel-cloudflare)
- [Comandi utili](#comandi-utili)
- [Se qualcosa non va](#se-qualcosa-non-va)
- [Note](#note)
- [Per il gruppo: preparare il pacchetto da consegnare](#per-il-gruppo-preparare-il-pacchetto-da-consegnare)

---

## Avvio rapido (per il docente)

### 0. Cosa serve installato

- **Docker Desktop** (o Docker Engine + Compose v2). Consigliati almeno **8 GB** assegnati alla VM di Docker.
- **Android Studio** con un SDK Android e un emulatore *oppure* un telefono Android con debug USB.
- Nient'altro: Java, Maven e i database girano nei container.

### 1. Clona il repository

```bash
git clone <URL-del-repo> Enterprise-Project
cd Enterprise-Project
```

### 2. Metti i file di configurazione

Il repository **non contiene nessuna credenziale reale** (sono tutte in `.gitignore`). Il gruppo ti consegna **fuori da Git** un pacchetto con questi file già pronti, da copiare esattamente in queste posizioni:

| File / cartella | Dove va | A cosa serve |
|---|---|---|
| `.env` | `backend/.env` | Password e chiavi del backend (Keycloak admin, Cloudinary, chiave interna…) |
| cartella `cloudflared/` (con `config.yml` + `<UUID>.json`) | `backend/cloudflared/` | Credenziali del tunnel Cloudflare che espone Keycloak e l'api-gateway |
| `realm-export-4.json` | `backend/keycloak-import/realm-export-4.json` | Realm Keycloak `tripify` con SMTP Gmail e client secret Google reali |
| `local.properties` | `frontend/local.properties` | Config dell'app Android (URL backend/Keycloak, path dell'SDK) |
| `docker-compose.override.yml` *(opzionale)* | `backend/docker-compose.override.yml` | Limiti di memoria per le JVM: mettilo solo se il backend va in OOM (vedi sotto) |

> In `frontend/local.properties` cambia **solo** `sdk.dir` con il percorso del tuo Android SDK. `BACKEND_IP` e `KEYCLOAK_IP` puntano già ai tunnel della demo e vanno lasciati così.

### 3. Avvia il backend

```bash
cd backend
docker compose up --build -d
```

Il **primo avvio** scarica le immagini, compila i 6 microservizi e importa il realm Keycloak: può richiedere diversi minuti. Le volte successive sono molto più rapide (cache).

Controlla che sia tutto su:

```bash
docker compose ps
```

Tutti i servizi devono essere `Up`. Poi verifica che il gateway risponda:

```bash
curl -s -o /dev/null -w "%{http_code}\n" https://api.tripify.cloud/api/v1/catalog/items
```

Deve rispondere `200` (o `401`, comunque una risposta HTTP: significa che tunnel + gateway sono vivi).

### 4. Avvia l'app Android

1. In Android Studio: **Open** → seleziona la cartella `frontend/`.
2. Attendi il **Gradle sync** (se `local.properties` è a posto parte da solo).
3. Scegli un emulatore o collega un telefono e premi **Run** (▶).

L'app è già compilata contro `https://api.tripify.cloud` e `https://keycloak.tripify.cloud`: **funziona da qualsiasi rete**, non serve che telefono e PC siano sulla stessa WiFi.

### 5. Entra

Usa un utente di prova già presente nel realm:

- **Viaggiatore** — `demo@tripify.it` / `Demo1234!`
- **Organizzatore** — `organizer@tripify.it` / `Demo1234!`

Oppure **Accedi con Google**, oppure **registrati** (la verifica email è disattivata, entri subito).

---

## Utenti di prova

| Utente | Password | Ruolo | Cosa può fare |
|---|---|---|---|
| `demo@tripify.it` | `Demo1234!` | `ROLE_TRAVELER` | Cercare voli/hotel/attività, mettere nel carrello, checkout (pagamento simulato), vedere le prenotazioni, scrivere recensioni, chattare con gli organizzatori, salvare itinerari |
| `organizer@tripify.it` | `Demo1234!` | `ROLE_ORGANIZER` | Tutto quello del viaggiatore + pubblicare/gestire annunci (voli, hotel, attività), vedere le prenotazioni ricevute, rispondere alle recensioni e alle chat |

La registrazione in-app crea sempre un `ROLE_TRAVELER`.

---

## Architettura

| Servizio | Porta | Ruolo |
|---|---|---|
| api-gateway | 8080 | Punto di ingresso unico, instrada le richieste ai servizi sottostanti |
| user-auth-service | 8081 | Autenticazione (Keycloak) e profilo utente |
| catalog-service | 8082 | Catalogo voli/hotel/attività, disponibilità e blocchi temporanei (hold) |
| booking-service | 8083 | Carrello, checkout, pagamento (simulato) e gestione prenotazioni |
| communication-service | 8084 | Chat, notifiche, recensioni |
| itinerary-service | 8085 | Liste di viaggio salvate e condivise |
| Keycloak | 8180 | Identity provider (OAuth2/OIDC), realm `tripify` |
| RabbitMQ | 5672 (AMQP) / 15672 (UI) | Coda di messaggi per le notifiche |
| Postgres | uno schema per servizio | Persistenza dati |
| cloudflared | — | Tunnel Cloudflare stabile: espone `keycloak.tripify.cloud` e `api.tripify.cloud` |

Ogni servizio backend valida da solo il token JWT emesso da Keycloak; le chiamate tra servizi propagano il token dell'utente, tranne la compensazione degli hold tra booking-service e catalog-service (pattern saga), che usa una chiave di servizio interna condivisa (`INTERNAL_SERVICE_KEY`).

L'app Android parla **solo** con l'api-gateway (`https://api.tripify.cloud`) e con Keycloak (`https://keycloak.tripify.cloud`). Un solo container `cloudflared` fa da tunnel per entrambi gli hostname, così il telefono raggiunge il backend da qualunque rete (WebSocket di chat e notifiche inclusi).

---

## Struttura del repository

```
Enterprise-Project/
├── backend/
│   ├── docker-compose.yml                 # infrastruttura + microservizi
│   ├── docker-compose.override.yml.example # limiti di memoria JVM (opzionale)
│   ├── .env.example                       # variabili d'ambiente da configurare
│   ├── keycloak-import.example.json       # template del realm "tripify" (segreti = CHANGEME)
│   ├── keycloak-import/                   # dove va messo il realm vero (fuori da Git)
│   ├── keycloak-themes/                   # tema di login/registrazione ed email personalizzato
│   ├── cloudflared/                       # config del tunnel Cloudflare (fuori da Git)
│   ├── api-gateway/
│   ├── booking-service/
│   ├── catalog-service/
│   ├── communication-service/
│   ├── itinerary-service/
│   └── user-auth-service/
└── frontend/                              # app Android (Kotlin + Jetpack Compose)
    └── local.properties.example           # config dell'app da copiare in local.properties
```

---

## Configurazione dettagliata

Serve solo a chi vuole capire cosa c'è dentro `.env` o ricreare la configurazione da zero. Per far girare la demo con i file consegnati **non serve toccare niente**.

```bash
cd backend
cp .env.example .env
```

| Variabile | Obbligatoria | A cosa serve | Da dove prenderla |
|---|---|---|---|
| `DB_PASSWORD` | no (default `password`) | Password di tutti i Postgres | Va bene il default per uso locale/demo |
| `KEYCLOAK_ADMIN_USERNAME` | no (default `admin`) | Utente admin di Keycloak (console + Admin Client di user-auth-service) | Va bene il default |
| `KEYCLOAK_ADMIN_PASSWORD` | **sì** | Password admin di Keycloak: user-auth-service la usa per sincronizzare nome/ruolo/attributi e per reset password / eliminazione account | Una password **forte** (non `admin`), la stessa della console Keycloak |
| `KEYCLOAK_PUBLIC_URL` | **sì** | URL HTTPS pubblico di Keycloak: è l'`iss` atteso nei JWT da app **e** backend. Senza, lo stack parte ma ogni chiamata autenticata torna 401 | `https://keycloak.tripify.cloud` (il tunnel Cloudflare) |
| `CLOUDFLARE_TUNNEL_TOKEN` | no | Solo se si usa un tunnel gestito da dashboard Zero Trust. Vuoto ⇒ si usa `backend/cloudflared/config.yml` | Vuoto per la demo |
| `LOCAL_IP` | **sì (non vuota)** | Campo obbligatorio: `docker compose up` si rifiuta di partire se manca. **Sotto Docker il valore non viene usato** (l'`iss` lo impone `KEYCLOAK_PUBLIC_URL`); serve solo se si lancia un servizio a mano fuori da Docker. Metti pure l'IP LAN della macchina, o `localhost` | `ipconfig getifaddr en0` (macOS) / `ipconfig` (Windows), oppure `localhost` |
| `RABBITMQ_*` | no (default `guest`/`guest`) | Credenziali RabbitMQ | Va bene il default per uso locale/demo |
| `INTERNAL_SERVICE_KEY` | **sì (non vuota)** | Chiave condivisa tra booking-service, catalog-service e communication-service: deve essere **identica** sui tre | Qualsiasi stringa concordata; per generarla `openssl rand -hex 32` |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | solo per l'upload immagini | Credenziali Cloudinary del gruppo (foto profilo/annunci) | Dashboard Cloudinary del gruppo |
| `GATEWAY_RATE_LIMIT_*` | no | Limite di richieste/minuto sul gateway | Va bene il default |

L'app Android legge la sua configurazione da `frontend/local.properties` (vedi `frontend/local.properties.example`): `sdk.dir`, `BACKEND_IP`, `KEYCLOAK_IP`, `MAPS_API_KEY` (opzionale). I valori finiscono in `BuildConfig` a **compile-time**: se cambi `BACKEND_IP` o `KEYCLOAK_IP` devi **ricompilare** l'app.

---

## Il tunnel Cloudflare

Keycloak **deve** stare dietro un URL HTTPS con hostname (non IP, non `http`): l'IdP Google rifiuta i redirect altrimenti, e il claim `iss` del token dev'essere identico per app e backend. Il compose include un container `cloudflared` che, con le credenziali in `backend/cloudflared/`, apre **un solo tunnel stabile con due hostname**:

- `keycloak.tripify.cloud` → `keycloak:8080`
- `api.tripify.cloud` → `api-gateway:8080`

Le regole stanno in `backend/cloudflared/config.yml`; `backend/cloudflared/<UUID>.json` contiene il segreto del tunnel. Tutta la cartella è gitignorata.

> Solo **una macchina alla volta** può tenere su il tunnel con queste credenziali. Durante la demo, il tunnel lo tiene su chi esegue `docker compose up`.

Per ricreare il tunnel da zero (da terminale, senza dashboard né carta di credito):

```bash
brew install cloudflared            # oppure il pacchetto per il tuo OS
cloudflared tunnel login            # scegli la tua zona, Authorize
cloudflared tunnel create tripify-keycloak
cloudflared tunnel route dns tripify-keycloak keycloak.tuo-dominio.xyz
cloudflared tunnel route dns tripify-keycloak api.tuo-dominio.xyz
cp ~/.cloudflared/<UUID>.json backend/cloudflared/
```

`backend/cloudflared/config.yml`:

```yaml
tunnel: <UUID>
credentials-file: /etc/cloudflared/<UUID>.json
ingress:
  - hostname: keycloak.tuo-dominio.xyz
    service: http://keycloak:8080
  - hostname: api.tuo-dominio.xyz
    service: http://api-gateway:8080
  - service: http_status:404
```

Poi aggiorna `KEYCLOAK_PUBLIC_URL` in `backend/.env` e `KEYCLOAK_IP` / `BACKEND_IP` in `frontend/local.properties` con i nuovi hostname, aggiungi `https://keycloak.tuo-dominio.xyz/realms/tripify/broker/google/endpoint` ai redirect URI autorizzati della Google Console, e **ricompila l'app**.

---

## Comandi utili

Da dentro `backend/`:

```bash
docker compose ps                       # stato dei servizi
docker compose logs -f booking-service  # log di un servizio (sostituisci il nome)
docker compose logs -f cloudflared      # log del tunnel
docker compose down                     # ferma tutto (dati nei volumi preservati)
docker compose down -v                  # ferma tutto e CANCELLA i dati (incluso il realm importato)
docker compose up --build -d            # ricostruisce e riavvia
```

La console di Keycloak è su `http://localhost:8180` (utente/password = `KEYCLOAK_ADMIN_USERNAME` / `KEYCLOAK_ADMIN_PASSWORD` del `.env`). La UI di RabbitMQ è su `http://localhost:15672` (`guest`/`guest`).

---

## Se qualcosa non va

| Sintomo | Causa probabile | Cosa fare |
|---|---|---|
| Ogni chiamata autenticata torna **401**, lo stack è "verde" | Il tunnel Keycloak non è su, oppure `KEYCLOAK_PUBLIC_URL` non combacia | `docker compose logs cloudflared`; verifica che `KEYCLOAK_PUBLIC_URL` in `.env` sia `https://keycloak.tripify.cloud` |
| L'app non raggiunge il backend (timeout ovunque) | Tunnel `api.tripify.cloud` giù, o `BACKEND_IP` sbagliato | `curl https://api.tripify.cloud/api/v1/catalog/items` dal PC; controlla `BACKEND_IP` in `local.properties` e **ricompila** l'app |
| `docker compose up` fa crashare Docker o i servizi vengono uccisi (OOM) | Le 6 JVM in over-commit sulla VM di Docker | `cp docker-compose.override.yml.example docker-compose.override.yml`, poi `docker compose up -d` |
| `port is already allocated` (8080/8180/5432/5672…) | Un altro processo occupa la porta | Ferma il processo in conflitto, oppure cambia il mapping in `docker-compose.yml` |
| `docker compose up` si rifiuta di partire con un errore su `LOCAL_IP` / `INTERNAL_SERVICE_KEY` / `KEYCLOAK_PUBLIC_URL` | Quella variabile è vuota in `.env` | Valorizzala (vedi tabella sopra) |
| Keycloak riparte in loop al primo avvio | Import del realm fallito | Controlla che `backend/keycloak-import/realm-export-4.json` esista e sia valido; `docker compose down -v` e riprova |
| Il Gradle sync in Android Studio fallisce | `sdk.dir` mancante o sbagliato in `local.properties` | Metti il percorso reale del tuo Android SDK |
| Login con Google dà errore di redirect | Redirect URI non registrato in Google Console | Deve esserci `https://keycloak.tripify.cloud/realms/tripify/broker/google/endpoint` tra i redirect autorizzati del client OAuth |

---

## Note

- Ogni microservizio ha il proprio `Dockerfile` multi-stage (toolchain Maven per compilare, immagine JRE leggera per l'esecuzione): `docker compose up --build` compila e avvia l'intero backend in un colpo solo, senza Java/Maven sulla macchina.
- Il **pagamento è simulato**: nessun gestore di pagamenti reale, qualunque carta che superi il controllo di formato/Luhn viene approvata.
- `INTERNAL_SERVICE_KEY` deve essere identica su booking-service, catalog-service e communication-service: senza, la compensazione degli hold tra booking e catalog non si autentica.
- Il realm importa già i due utenti di prova (`demo@tripify.it`, `organizer@tripify.it`, password `Demo1234!`): si entra subito senza registrarsi.
- L'import del realm avviene **solo al primo avvio con database vuoto**. Dopo `docker compose down -v` viene rifatto; con un semplice `docker compose down` il realm resta.

---

## Per il gruppo: preparare il pacchetto da consegnare

I file con i segreti reali sono gitignorati e non finiscono mai su Git. Per la consegna vanno passati al docente **fuori da Git** (email, piattaforma del corso, chiavetta). Dalla root del repo:

```bash
tar -czf tripify-config.tgz \
  backend/.env \
  backend/cloudflared \
  backend/keycloak-import/realm-export-4.json \
  backend/docker-compose.override.yml \
  frontend/local.properties
```

Il docente scompatta `tripify-config.tgz` dentro la sua copia del repo (`tar -xzf tripify-config.tgz`) e i file finiscono già al posto giusto. Prima di generare l'archivio, assicurati che:

- `backend/keycloak-import/realm-export-4.json` abbia i valori **reali** (non `CHANGEME`) in `smtpServer.password` e nel `clientSecret` del provider `google`;
- `backend/.env` abbia `KEYCLOAK_ADMIN_PASSWORD`, `INTERNAL_SERVICE_KEY` e le tre variabili `CLOUDINARY_*` valorizzate;
- `backend/cloudflared/` contenga sia `config.yml` sia `<UUID>.json`;
- `frontend/local.properties` abbia `BACKEND_IP=https://api.tripify.cloud` e `KEYCLOAK_IP=https://keycloak.tripify.cloud` (il docente cambierà solo `sdk.dir`).
