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
│   ├── keycloak-import.example.json  # template del realm "tripify" (segreti = CHANGEME)
│   ├── keycloak-import/         # dove va messo il realm vero (fuori da Git), importato al primo avvio
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
| `KEYCLOAK_ADMIN_USERNAME` | no (default `admin`) | Utente admin di Keycloak (console + Admin Client di user-auth-service) | Va bene il default |
| `KEYCLOAK_ADMIN_PASSWORD` | **sì** | Password admin di Keycloak: user-auth-service la usa per sincronizzare nome/ruolo/attributi e per reset password / delete account | Mettine una **forte** (non `admin`), la stessa della console Keycloak |
| `KEYCLOAK_PUBLIC_URL` | **sì** | URL HTTPS pubblico di Keycloak: e' l'`iss` atteso nei JWT (app **e** backend). Senza, lo stack parte ma ogni chiamata autenticata torna 401 | L'URL del tunnel Cloudflare davanti a Keycloak (vedi nota sotto) |
| `CLOUDFLARE_TUNNEL_TOKEN` | no | Se valorizzato, `cloudflared` usa un **named tunnel su dominio tuo** (URL stabile). Se vuoto, tunnel "quick" con URL random che cambia a ogni riavvio | Cloudflare Zero Trust → Tunnels (vedi nota sotto) |
| `LOCAL_IP` | **sì** | IP LAN della macchina: serve **solo a booking-service** per le notifiche RabbitMQ | `ipconfig getifaddr en0` (macOS) / `ipconfig` (Windows) |
| `RABBITMQ_*` | no (default `guest`/`guest`) | Credenziali RabbitMQ | Va bene il default per uso locale/demo |
| `INTERNAL_SERVICE_KEY` | no (default `dev-internal-key` in `.env.example`) | Chiave condivisa tra booking-service, catalog-service e communication-service: deve essere **identica** sui tre | Il default va bene per demo; altrimenti `openssl rand -hex 32` |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | solo per l'upload immagini | Credenziali Cloudinary del gruppo (foto profilo/annunci, user-auth-service e catalog-service) | Dashboard Cloudinary del gruppo |
| `GATEWAY_RATE_LIMIT_*` | no | Limite di richieste/minuto sul gateway | Va bene il default |

> **Keycloak deve stare dietro un URL HTTPS con hostname** (non IP, non `http`): l'IdP Google rifiuta i redirect altrimenti, e il claim `iss` del token dev'essere identico per app e backend. Il compose include un container `cloudflared` che espone `keycloak:8080`. Due modi:
>
> **A) Tunnel "quick" (gratis, zero setup, URL instabile).** Lascia `CLOUDFLARE_TUNNEL_TOKEN` vuoto. Primo `docker compose up -d`, poi leggi l'URL da `docker compose logs cloudflared` (`https://<parole-a-caso>.trycloudflare.com`) e mettilo in `KEYCLOAK_PUBLIC_URL`, poi `docker compose up -d --no-deps` sui servizi Java. **L'URL cambia ogni volta che `cloudflared` riparte** → non riavviarlo durante la demo.
>
> **B) Named tunnel su dominio proprio (URL stabile, ~1 €/anno).** Registra un dominio (es. `.xyz`, `.cloud`) e aggiungilo a Cloudflare (piano free, nameserver puntati a Cloudflare). Poi, **da terminale, senza dashboard né carta**:
> ```bash
> brew install cloudflared
> cloudflared tunnel login                                  # scegli la tua zona, Authorize
> cloudflared tunnel create tripify-keycloak                # annota l'UUID
> cloudflared tunnel route dns tripify-keycloak keycloak.tuo-dominio.xyz
> cp ~/.cloudflared/<UUID>.json backend/cloudflared/
> ```
> Crea `backend/cloudflared/config.yml`:
> ```yaml
> tunnel: <UUID>
> credentials-file: /etc/cloudflared/<UUID>.json
> ingress:
>   - hostname: keycloak.tuo-dominio.xyz
>     service: http://keycloak:8080
>   - service: http_status:404
> ```
> Metti `KEYCLOAK_PUBLIC_URL=https://keycloak.tuo-dominio.xyz` e lascia `CLOUDFLARE_TUNNEL_TOKEN` vuoto. `backend/cloudflared/` è gitignored (contiene il segreto del tunnel). In alternativa, con un metodo di pagamento sul profilo Cloudflare, si crea il tunnel dalla dashboard Zero Trust e si mette solo il `CLOUDFLARE_TUNNEL_TOKEN` (vedi `.env.example`).
>
> **In entrambi i casi** `KEYCLOAK_PUBLIC_URL` va copiato identico in `frontend/local.properties` (`KEYCLOAK_IP`) e nel redirect URI della Google Console (`https://<host>/realms/tripify/broker/google/endpoint`), e l'app Android va **ricompilata** dopo ogni cambio (i valori finiscono in `BuildConfig` a compile-time). Col modo B lo fai una volta sola.

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

# Host del backend (api-gateway). Con un valore che contiene "ngrok" viene
# usato come https://<host>; con "http" viene preso cosi' com'e'; altrimenti
# diventa http://<host>:8080. Da emulatore puoi usare 10.0.2.2.
BACKEND_IP=192.168.1.50

# URL di Keycloak: DEVE combaciare con KEYCLOAK_PUBLIC_URL in backend/.env.
# Con un valore che contiene "http" viene usato tale e quale (e' il caso del
# tunnel Cloudflare), es. KEYCLOAK_IP=https://keycloak.tuo-dominio.xyz
KEYCLOAK_IP=https://keycloak.tuo-dominio.xyz

# Opzionale: serve solo per le mappe statiche nel dettaglio di un annuncio.
MAPS_API_KEY=
```

Poi apri il progetto in Android Studio (o esegui `./gradlew assembleDebug`) e avvialo su un emulatore o dispositivo.

## Consegnare le chiavi al docente

Il repository **non contiene nessuna credenziale reale**: `.env`, `local.properties` e il realm Keycloak con i segreti dentro sono nel `.gitignore` apposta, così un push non li fa mai finire su Git. Perché il docente possa far girare il progetto senza doversi creare account propri, il gruppo deve consegnargli **fuori da Git** (email, piattaforma del corso, chiavetta) tre file già pronti:

1. **`backend/.env`** — con: `KEYCLOAK_PUBLIC_URL` (URL HTTPS del tunnel Cloudflare davanti a Keycloak) e, se si usa il dominio stabile, `CLOUDFLARE_TUNNEL_TOKEN`; `KEYCLOAK_ADMIN_PASSWORD` a una password forte; `LOCAL_IP` all'IP LAN della macchina della demo; `INTERNAL_SERVICE_KEY` a una stringa concordata; le tre variabili Cloudinary con le credenziali reali del gruppo. Il resto può restare ai default di `.env.example`.
2. **`frontend/local.properties`** — con `KEYCLOAK_IP` **identico** a `KEYCLOAK_PUBLIC_URL` del punto 1 e `BACKEND_IP` all'host del gateway.
3. **`backend/keycloak-import/realm-export-4.json`** — il realm `tripify` con la password SMTP Gmail e il client secret Google reali. Nel repo c'è solo il template `backend/keycloak-import.example.json` con quei due campi a `CHANGEME`: per ottenere il file vero, `mkdir -p backend/keycloak-import && cp backend/keycloak-import.example.json backend/keycloak-import/realm-export-4.json`, poi sostituire i due valori (`smtpServer.password` e il `clientSecret` del provider `google`). La cartella `backend/keycloak-import/` è gitignorata: il realm con i segreti non finisce mai su Git.

Con questi tre file al posto giusto, il docente deve solo eseguire `docker compose up --build -d` in `backend/` e aprire/buildare l'app in `frontend/`: nessun'altra configurazione manuale. Il realm importa già due utenti di prova (`demo@tripify.it` e `organizer@tripify.it`, password `Demo1234!`), quindi si può entrare subito senza registrarsi.

## Note

- Ogni microservizio ha il proprio `Dockerfile` multi-stage (toolchain Maven per compilare, immagine JRE leggera per l'esecuzione): `docker compose up` compila e avvia l'intero backend in un colpo solo, senza bisogno di Java/Maven installati sulla macchina.
- Il pagamento è simulato: nessun collegamento a un vero gestore di pagamenti, qualunque numero di carta che superi il controllo di formato/Luhn viene approvato.
- `INTERNAL_SERVICE_KEY` deve essere identica su booking-service, catalog-service e communication-service: senza, la compensazione degli hold tra booking e catalog non si autentica correttamente.
