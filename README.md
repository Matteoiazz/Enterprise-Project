# Tripify

Piattaforma di prenotazione viaggi (voli, hotel, attività) a microservizi: backend Spring Boot + app Android nativa (Kotlin, Jetpack Compose).

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

Ogni servizio backend valida da solo il token JWT emesso da Keycloak; le chiamate tra servizi propagano il token dell'utente, tranne la compensazione degli hold tra booking-service e catalog-service (pattern saga), che usa una chiave di servizio interna condivisa.

## Struttura del repository

```
Enterprise-Project/
├── backend/
│   ├── docker-compose.yml       # infrastruttura + microservizi
│   ├── .env.example             # variabili d'ambiente da configurare
│   ├── keycloak-import/         # realm "tripify", importato al primo avvio
│   ├── keycloak-themes/         # tema di login/registrazione personalizzato
│   ├── api-gateway/
│   ├── booking-service/
│   ├── catalog-service/
│   ├── communication-service/
│   ├── itinerary-service/
│   └── user-auth-service/
└── frontend/                    # app Android (Kotlin + Jetpack Compose)
```

## Prerequisiti

- Docker e Docker Compose
- Per l'app Android: Android Studio (o solo Gradle) e un emulatore/dispositivo
- Nient'altro in locale: Java, Maven e i database girano tutti nei container

## 1. Configurazione

Il backend legge la configurazione da variabili d'ambiente: default di sviluppo dove ha senso (password, credenziali interne al gruppo), nessun default per le credenziali di servizi esterni reali che non si possono indovinare.

```bash
cd backend
cp .env.example .env
```

Poi apri `.env` e valorizza le variabili:

| Variabile | Obbligatoria | A cosa serve | Da dove prenderla |
|---|---|---|---|
| `DB_PASSWORD` | no (default `password`) | Password di tutti i Postgres | Va bene il default per uso locale/demo |
| `KEYCLOAK_ADMIN_USERNAME` / `KEYCLOAK_ADMIN_PASSWORD` | no (default `admin`/`admin`) | Credenziali admin di Keycloak (console + Admin Client usato da user-auth-service) | Va bene il default per uso locale/demo |
| `LOCAL_IP` | **sì** | Host con cui backend **e** app Android raggiungono Keycloak per validare i JWT | L'IP LAN reale della macchina che fa girare `docker compose` (vedi nota sotto) |
| `RABBITMQ_*` | no (default `guest`/`guest`) | Credenziali RabbitMQ | Va bene il default per uso locale/demo |
| `INTERNAL_SERVICE_KEY` | no (default `dev-internal-key` in `.env.example`) | Chiave condivisa tra booking-service, catalog-service e communication-service per la compensazione degli hold: deve essere **identica** sui tre | Il default va bene per demo; altrimenti una stringa a caso, es. `openssl rand -hex 32` |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | solo per l'upload immagini | Credenziali Cloudinary del gruppo (foto profilo e foto annunci, usate da user-auth-service e catalog-service) | Dashboard Cloudinary del gruppo (cloudinary.com/console) |
| `GATEWAY_RATE_LIMIT_*` | no | Limite di richieste/minuto sul gateway | Va bene il default |

> **Nota su `LOCAL_IP`**: non usare `localhost` né il nome del servizio Docker (`keycloak`). Il token emesso da Keycloak porta come issuer l'indirizzo con cui il *client* lo ha contattato; l'app Android sta fuori dalla rete Docker, quindi backend e app devono raggiungere Keycloak allo **stesso indirizzo**, altrimenti la validazione del JWT fallisce per issuer diverso. Usa l'IP LAN reale del PC (es. `192.168.1.50`) — lo stesso da mettere anche in `frontend/local.properties` come `KEYCLOAK_IP`.

> **Se cambi rete / l'IP della macchina cambia**: `LOCAL_IP` va tenuto allineato in tre punti, poi va **ricompilata l'app** (i valori finiscono in `BuildConfig` a compile-time):
> 1. `backend/.env` → `LOCAL_IP`
> 2. `frontend/local.properties` → `KEYCLOAK_IP` (e `BACKEND_IP`, se non usi un tunnel come ngrok)
> 3. `frontend/app/src/main/res/xml/network_security_config.xml` → aggiungi il nuovo IP alla whitelist `<domain-config>`
>
> L'IP LAN si ricava con `ipconfig getifaddr en0` (macOS) o `ipconfig` (Windows). Dopo la modifica: `docker compose down && docker compose up -d` e rebuild dell'app.

## 2. Avvio del backend

```bash
cd backend
docker compose up --build -d
```

Il primo avvio scarica le immagini, compila ogni microservizio e importa il realm Keycloak: può richiedere qualche minuto. Le volte successive sono molto più veloci grazie alla cache.

Per controllare lo stato:

```bash
docker compose ps
docker compose logs -f booking-service   # sostituisci con il servizio che ti interessa
```

Quando tutti i servizi sono `Up`, l'API è raggiungibile su `http://localhost:8080` (api-gateway) e la console di Keycloak su `http://localhost:8180`.

Per fermare tutto (dati nei volumi preservati):

```bash
docker compose down
```

Per ripartire da zero (cancella anche i dati, incluso il realm importato):

```bash
docker compose down -v
```

## 3. Avvio dell'app Android

Crea `frontend/local.properties` (non è versionato, va creato da chi clona il progetto):

```properties
sdk.dir=/percorso/del/tuo/Android/sdk

# IP della macchina che fa girare il backend. Da un emulatore Android puoi
# usare 10.0.2.2 (alias dell'host) SOLO se anche LOCAL_IP nel backend punta
# a un indirizzo raggiungibile dall'emulatore; da un dispositivo fisico sulla
# stessa rete, usa l'IP LAN del PC (lo stesso di LOCAL_IP in backend/.env).
BACKEND_IP=192.168.1.50
KEYCLOAK_IP=192.168.1.50

# Opzionale: serve solo per le mappe statiche nel dettaglio di un annuncio.
MAPS_API_KEY=
```

Poi apri il progetto in Android Studio (o esegui `./gradlew assembleDebug`) e avvialo su un emulatore o dispositivo.

## Consegnare le chiavi al docente

Il repository **non contiene nessuna credenziale reale**: `.env` e `local.properties` sono nel `.gitignore` apposta, così un push non li fa mai finire su Git. Perché il docente possa far girare il progetto senza doversi creare un proprio account Cloudinary, il gruppo deve consegnargli **fuori da Git** (email, piattaforma del corso, chiavetta) due file già pronti:

1. **`backend/.env`** — con `LOCAL_IP` impostato all'IP della macchina su cui girerà la demo, `INTERNAL_SERVICE_KEY` a una stringa qualsiasi concordata, e le tre variabili Cloudinary valorizzate con le credenziali reali dell'account del gruppo. Il resto può restare ai default di `.env.example`.
2. **`frontend/local.properties`** — con `BACKEND_IP`/`KEYCLOAK_IP` coerenti con lo stesso `LOCAL_IP` usato nel punto 1.

Con questi due file al posto giusto, il docente deve solo eseguire `docker compose up --build -d` in `backend/` e aprire/buildare l'app in `frontend/`: nessun'altra configurazione manuale.

## Note

- Ogni microservizio ha il proprio `Dockerfile` multi-stage (toolchain Maven per compilare, immagine JRE leggera per l'esecuzione): `docker compose up` compila e avvia l'intero backend in un colpo solo, senza bisogno di Java/Maven installati sulla macchina.
- Il pagamento è simulato: nessun collegamento a un vero gestore di pagamenti, qualunque numero di carta che superi il controllo di formato/Luhn viene approvato.
- `INTERNAL_SERVICE_KEY` deve essere identica su booking-service, catalog-service e communication-service: senza, la compensazione degli hold tra booking e catalog non si autentica correttamente.
