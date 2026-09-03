# Tripify

Piattaforma di prenotazione viaggi (voli, hotel, attività) a **microservizi**: backend Spring Boot + app **Android nativa** (Kotlin, Jetpack Compose).

Il backend gira **tutto in Docker** — 6 microservizi, un database Postgres per servizio, Keycloak, RabbitMQ e il tunnel: non serve installare Java o Maven. L'app Android si compila con Android Studio.

> **Prima di iniziare.** Il codice sta nel repo, ma le **credenziali no**: `.env`, `local.properties`, il realm Keycloak e le chiavi del tunnel sono in `.gitignore` (il repo è pubblico). Avere accesso al repo **non basta**: serve anche l'archivio **`tripify-config.tgz`**, consegnato a parte dal gruppo (email / piattaforma del corso). Senza quello lo stack non parte. Vedi [passo 2](#avvio-rapido-per-il-docente).

---

## Indice

1. [Come è fatto](#come-è-fatto)
2. [Prerequisiti](#prerequisiti)
3. [Avvio rapido (per il docente)](#avvio-rapido-per-il-docente)
4. [I file di configurazione in dettaglio](#i-file-di-configurazione-in-dettaglio)
5. [Utenti di prova e ruoli](#utenti-di-prova-e-ruoli)
6. [Il tunnel Cloudflare](#il-tunnel-cloudflare)
7. [Porte, console e strumenti](#porte-console-e-strumenti)
8. [Comandi utili](#comandi-utili)
9. [Smoke test: "funziona se…"](#smoke-test-funziona-se)
10. [Se qualcosa non va](#se-qualcosa-non-va)
11. [Note tecniche](#note-tecniche)
12. [Struttura del repository](#struttura-del-repository)
13. [Per il gruppo: preparare il pacchetto da consegnare](#per-il-gruppo-preparare-il-pacchetto-da-consegnare)

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

**App Android.** Parla **solo** con l'api-gateway (`https://api.tripify.cloud`) e con Keycloak (`https://keycloak.tripify.cloud`). Chat e notifiche usano WebSocket/STOMP su `wss://api.tripify.cloud/ws-chat`. Gli URL finiscono in `BuildConfig` a **compile-time**: se cambiano, l'app va ricompilata.

---

## Prerequisiti

| Cosa | Note |
|---|---|
| **Docker Desktop** (o Docker Engine + Compose v2) | Assegna alla VM di Docker almeno **8 GB di RAM** e ~10 GB di disco liberi. Verifica: `docker compose version` |
| **Android Studio** (Ladybug o più recente) | Con un **SDK Android** (API 30+) e un emulatore, oppure un telefono Android con debug USB attivo |
| Connessione internet | Al primo avvio scarica immagini Docker e dipendenze Maven/Gradle |

Non serve altro: Java, Maven e i database girano nei container. Android Studio porta con sé il JDK per compilare l'app.

---

## Avvio rapido (per il docente)

### 1. Clona il repository

```bash
git clone <URL-del-repo> Enterprise-Project
cd Enterprise-Project
```

### 2. Metti i file di configurazione

Il repository **non contiene nessuna credenziale reale**: sono tutte in `.gitignore`. Il gruppo consegna **fuori da Git** un archivio (`tripify-config.tgz`) con i file già pronti. Scompattalo nella radice del repo:

```bash
tar -xzf tripify-config.tgz
```

I file finiscono automaticamente qui:

| File / cartella | Percorso | Contiene |
|---|---|---|
| `.env` | `backend/.env` | Password e chiavi del backend (Keycloak admin, Cloudinary, chiave interna…) |
| cartella `cloudflared/` | `backend/cloudflared/` | `config.yml` + `<UUID>.json`: credenziali del tunnel Cloudflare |
| `realm-export-4.json` | `backend/keycloak-import/realm-export-4.json` | Realm Keycloak `tripify` con SMTP Gmail e client secret Google |
| `docker-compose.override.yml` | `backend/docker-compose.override.yml` | Limiti di memoria per le JVM *(opzionale: serve solo se il backend va in OOM)* |
| `local.properties` | `frontend/local.properties` | Config dell'app Android |

> **Unica cosa da modificare a mano:** in `frontend/local.properties` metti il percorso del **tuo** Android SDK in `sdk.dir`. `BACKEND_IP` e `KEYCLOAK_IP` puntano già ai tunnel della demo, lasciali così.

### 3. Avvia il backend

```bash
cd backend
docker compose up --build -d
```

Il **primo avvio** scarica le immagini, **compila i 6 microservizi** (Maven, dentro i container) e **importa il realm Keycloak**: metti in conto **5–10 minuti**. Gli avvii successivi sono nell'ordine dei secondi grazie alla cache.

Segui l'avanzamento:

```bash
docker compose logs -f
```

Quando si calma, controlla lo stato:

```bash
docker compose ps
```

Tutti i servizi devono essere `Up` (i database e alcuni servizi mostrano anche `healthy`). Poi verifica che il tunnel del backend arrivi al gateway:

```bash
curl -si https://api.tripify.cloud/api/v1/catalog/items/search | head -5
```

Deve rispondere con header HTTP e un corpo JSON (anche un `200` con lista vuota o un `400`: l'importante è che risponda **il backend**, non un errore di Cloudflare).

### 4. Compila e avvia l'app Android

1. In Android Studio: **Open** → seleziona la cartella `frontend/`.
2. Aspetta il **Gradle sync** (parte da solo se `local.properties` è a posto).
3. Scegli un emulatore o collega un telefono e premi **Run** (▶).

L'app è compilata contro `https://api.tripify.cloud` e `https://keycloak.tripify.cloud`: **funziona da qualsiasi rete**, non serve che telefono e PC siano sulla stessa WiFi.

### 5. Entra

- **Viaggiatore** — email `demo@tripify.it`, password `Demo1234!`
- **Organizzatore** — email `organizer@tripify.it`, password `Demo1234!`

Oppure **Accedi con Google**, oppure **Registrati** (la verifica email è disattivata: entri subito).

---

## I file di configurazione in dettaglio

Per far girare la demo con l'archivio consegnato **non serve toccare niente** (a parte `sdk.dir`). Questa sezione serve solo a capire cosa c'è dentro o a ricreare la configurazione da zero (`cp backend/.env.example backend/.env`).

### `backend/.env`

| Variabile | Obbligatoria | A cosa serve | Valore per la demo |
|---|---|---|---|
| `DB_PASSWORD` | no (default `password`) | Password di tutti i Postgres | Il default va bene |
| `KEYCLOAK_ADMIN_USERNAME` | no (default `admin`) | Utente admin di Keycloak (console + Admin Client) | `admin` |
| `KEYCLOAK_ADMIN_PASSWORD` | **sì** | user-auth-service la usa per sincronizzare profilo/ruoli e per reset password / eliminazione account | Una password forte, la stessa della console Keycloak |
| `KEYCLOAK_PUBLIC_URL` | **sì** | È l'`iss` atteso nei JWT da app **e** backend. Se non combacia, ogni chiamata autenticata torna 401 | `https://keycloak.tripify.cloud` |
| `CLOUDFLARE_TUNNEL_TOKEN` | no | Solo per un tunnel gestito da dashboard Zero Trust. Vuoto ⇒ si usa `backend/cloudflared/config.yml` | vuoto |
| `LOCAL_IP` | **sì (non vuota)** | Campo obbligatorio: `docker compose up` si rifiuta di partire se manca. **Sotto Docker il valore non viene usato** (l'`iss` lo impone `KEYCLOAK_PUBLIC_URL`); conta solo se lanci un servizio a mano fuori da Docker | l'IP LAN della macchina, oppure `localhost` |
| `INTERNAL_SERVICE_KEY` | **sì (non vuota)** | Chiave condivisa per la compensazione saga: **identica** su booking, catalog e communication | Una stringa qualsiasi concordata (`openssl rand -hex 32`) |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | no (default `guest`) | Credenziali RabbitMQ | Il default va bene |
| `RABBITMQ_ERLANG_COOKIE` | no | Cookie di cluster RabbitMQ | Il default va bene |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | solo per l'upload immagini | Foto profilo e foto annunci | Credenziali dell'account Cloudinary del gruppo |
| `GATEWAY_RATE_LIMIT_ENABLED` / `GATEWAY_RATE_LIMIT_MAX` | no | Rate limiting sul gateway | Il default va bene |

### `frontend/local.properties`

Vedi `frontend/local.properties.example`.

| Voce | Valore per la demo | Note |
|---|---|---|
| `sdk.dir` | *(il tuo path)* | `~/Library/Android/sdk` (macOS), `~/Android/Sdk` (Linux), `C:\Users\<tu>\AppData\Local\Android\Sdk` (Windows) |
| `BACKEND_IP` | `https://api.tripify.cloud` | Se contiene `http` è usato tale e quale; altrimenti diventa `http://<valore>:8080` |
| `KEYCLOAK_IP` | `https://keycloak.tripify.cloud` | Stessa regola; deve combaciare con `KEYCLOAK_PUBLIC_URL` |
| `MAPS_API_KEY` | *(opzionale)* | Chiave Google Static Maps, solo per la mini-mappa nel dettaglio annuncio |

### `backend/keycloak-import/realm-export-4.json`

Il realm `tripify` completo: client dell'app (`tripify-android-client`, public + PKCE S256, redirect `com.tripify.app://oauth`), provider Google, SMTP Gmail, tema `tripify`, i due utenti di prova. Nel repo c'è solo il template `backend/keycloak-import.example.json` con i segreti a `CHANGEME`. Il file vero si ottiene così:

```bash
mkdir -p backend/keycloak-import
cp backend/keycloak-import.example.json backend/keycloak-import/realm-export-4.json
```

poi si sostituiscono i due valori `CHANGEME`:
- `smtpServer.password` → app-password del Gmail `tripify.noreply@gmail.com`
- `identityProviders[google].config.clientSecret` → client secret dell'OAuth client Google

La cartella `backend/keycloak-import/` è gitignorata: il realm coi segreti non finisce mai su Git.

### `backend/docker-compose.override.yml` (opzionale)

Vedi `backend/docker-compose.override.yml.example`. Docker Compose lo carica in automatico sopra `docker-compose.yml`. Mettilo **solo** se `docker compose up` fa crashare la VM di Docker o i servizi vengono uccisi per OOM: impone un `-Xmx` a ogni JVM così le 6 insieme non vanno in over-commit.

```bash
cp backend/docker-compose.override.yml.example backend/docker-compose.override.yml
```

---

## Utenti di prova e ruoli

| Utente | Password | Ruolo | Cosa può fare |
|---|---|---|---|
| `demo@tripify.it` | `Demo1234!` | `ROLE_TRAVELER` | Cercare voli/hotel/attività, filtrare, mettere nel carrello, checkout (pagamento simulato), vedere le proprie prenotazioni, scrivere e votare recensioni, chattare con gli organizzatori, ricevere notifiche, creare e condividere itinerari, gestire profilo / carte / documenti / compagni di viaggio |
| `organizer@tripify.it` | `Demo1234!` | `ROLE_ORGANIZER` | Tutto quello del viaggiatore **+** pubblicare e gestire annunci (voli, hotel, attività), caricarne le foto, disattivarli/riattivarli, vedere le prenotazioni ricevute, rispondere alle recensioni e alle chat |

La registrazione in-app crea sempre un `ROLE_TRAVELER`. Il login con Google crea l'utente al primo accesso.

---

## Il tunnel Cloudflare

Keycloak **deve** stare dietro un URL **HTTPS con hostname** (non IP, non `http`): l'IdP Google rifiuta i redirect altrimenti, e il claim `iss` del token deve essere identico per app e backend. Il container `cloudflared` nel compose, con le credenziali in `backend/cloudflared/`, apre **un solo tunnel stabile con due hostname**:

- `keycloak.tripify.cloud` → `keycloak:8080`
- `api.tripify.cloud` → `api-gateway:8080`

Le regole sono in `backend/cloudflared/config.yml`; `backend/cloudflared/<UUID>.json` è il segreto del tunnel. Tutta la cartella è gitignorata.

> Con queste credenziali il tunnel lo può tenere su **una macchina alla volta**. Durante la demo lo tiene su chi esegue `docker compose up`.

### Ricreare il tunnel da zero

Da terminale, senza dashboard né carta di credito, serve solo un dominio su Cloudflare (piano free):

```bash
brew install cloudflared                       # o il pacchetto per il tuo OS
cloudflared tunnel login                       # scegli la tua zona, Authorize
cloudflared tunnel create tripify-keycloak     # annota l'UUID
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

Poi:
- `KEYCLOAK_PUBLIC_URL=https://keycloak.tuo-dominio.xyz` in `backend/.env`
- `KEYCLOAK_IP=https://keycloak.tuo-dominio.xyz` e `BACKEND_IP=https://api.tuo-dominio.xyz` in `frontend/local.properties`
- aggiungi `https://keycloak.tuo-dominio.xyz/realms/tripify/broker/google/endpoint` ai **redirect URI autorizzati** del client OAuth in Google Cloud Console
- **ricompila l'app**

Se cambi `config.yml` con lo stack già su, ricrea il container: `docker compose up -d --force-recreate cloudflared`.

---

## Porte, console e strumenti

| URL | Cosa |
|---|---|
| `https://api.tripify.cloud` | API pubblica (api-gateway) usata dall'app |
| `https://keycloak.tripify.cloud` | Keycloak pubblico (login, token, redirect Google) |
| `http://localhost:8080` | api-gateway diretto (dalla macchina della demo) |
| `http://localhost:8180` | Console admin di Keycloak — utente/password = `KEYCLOAK_ADMIN_USERNAME` / `KEYCLOAK_ADMIN_PASSWORD` |
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
docker compose up -d --build booking-service   # ricompila e riavvia un servizio
docker compose up -d --force-recreate cloudflared  # ricrea il tunnel (dopo modifiche a config.yml)
docker compose down                        # ferma tutto (dati nei volumi preservati)
docker compose down -v                     # ferma tutto e CANCELLA i dati (DB + realm importato)
```

---

## Smoke test: "funziona se…"

Dopo `docker compose up` e con l'app installata:

1. `docker compose ps` → tutti `Up`.
2. `curl -si https://keycloak.tripify.cloud/realms/tripify` → `200`.
3. `curl -si https://api.tripify.cloud/api/v1/catalog/items/search` → risponde il backend (JSON).
4. App: login con `demo@tripify.it` / `Demo1234!` → arrivi alla home.
5. App: una ricerca voli o hotel → compaiono risultati.
6. App: apri un annuncio, aggiungi al carrello, vai al checkout, inserisci una carta qualsiasi valida per formato (16 cifre, scadenza futura) → prenotazione confermata.
7. App: dalla prenotazione apri la chat con l'organizzatore, scrivi un messaggio → con l'altro utente (`organizer@tripify.it`) lo ricevi in tempo reale e arriva la notifica.
8. App: lascia una recensione sull'annuncio prenotato → compare nel dettaglio.

---

## Se qualcosa non va

| Sintomo | Causa probabile | Cosa fare |
|---|---|---|
| Ogni chiamata autenticata torna **401**, lo stack è "verde" | Tunnel Keycloak giù, o `KEYCLOAK_PUBLIC_URL` non combacia con l'`iss` | `docker compose logs cloudflared`; verifica `KEYCLOAK_PUBLIC_URL=https://keycloak.tripify.cloud` in `.env` |
| L'app va in timeout su tutto | Tunnel `api.tripify.cloud` giù, o `BACKEND_IP` sbagliato | `curl -si https://api.tripify.cloud/api/v1/catalog/items/search`; controlla `BACKEND_IP` in `local.properties` e **ricompila** |
| `curl` su `api.tripify.cloud` dà un errore Cloudflare (52x/1016) e non JSON | Il container `cloudflared` non ha le due regole di ingress | `docker compose up -d --force-recreate cloudflared`, poi `docker compose logs --tail=40 cloudflared` |
| `docker compose up` crasha o i servizi muoiono per OOM | Le 6 JVM in over-commit sulla VM di Docker | `cp docker-compose.override.yml.example docker-compose.override.yml`, poi `docker compose up -d` |
| `port is already allocated` (8080/8180/5433…) | Un altro processo occupa la porta | Ferma il processo in conflitto o cambia il mapping in `docker-compose.yml` |
| `docker compose up` si rifiuta di partire: errore su `LOCAL_IP` / `INTERNAL_SERVICE_KEY` / `KEYCLOAK_PUBLIC_URL` | Quella variabile è vuota in `.env` | Valorizzala (vedi tabella `.env`) |
| Keycloak riparte in loop al primo avvio | Import del realm fallito | Controlla che `backend/keycloak-import/realm-export-4.json` esista e sia JSON valido; `docker compose down -v` e riprova |
| Login con Google: errore `redirect_uri_mismatch` | Redirect URI non registrato in Google Console | Deve esserci `https://keycloak.tripify.cloud/realms/tripify/broker/google/endpoint` tra i redirect autorizzati del client OAuth |
| Le email (reset password, verifica) non arrivano | `smtpServer.password` nel realm è `CHANGEME` o l'app-password Gmail è scaduta | Rigenera l'app-password del Gmail e rimettila in `realm-export-4.json`, poi `docker compose down -v && docker compose up -d` |
| Gradle sync fallisce in Android Studio | `sdk.dir` mancante o sbagliato in `local.properties` | Metti il percorso reale del tuo Android SDK |
| Build dell'app fallisce con `Could not connect to Kotlin daemon` / `Storage ... is already registered` | Cache incrementale Kotlin corrotta | Android Studio chiuso: `cd frontend && ./gradlew --stop && rm -rf app/build/kotlin .gradle/*/kotlin`, poi **Rebuild** |
| `WARN Found orphan containers ([caddy])` | Container di una vecchia configurazione | `docker compose up -d --remove-orphans` |

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
│   ├── .env.example                          # variabili d'ambiente da configurare
│   ├── keycloak-import.example.json          # template del realm "tripify" (segreti = CHANGEME)
│   ├── keycloak-import/                      # dove va messo il realm vero (fuori da Git)
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

---

## Per il gruppo: preparare il pacchetto da consegnare

I file con i segreti reali sono gitignorati e non finiscono mai su Git. Per la consegna vanno passati al docente **fuori da Git** (email, piattaforma del corso, chiavetta).

**Prima di generare l'archivio**, assicurati che:

- [ ] `backend/keycloak-import/realm-export-4.json` abbia i valori **reali** (non `CHANGEME`) in `smtpServer.password` e nel `clientSecret` del provider `google`
- [ ] `backend/.env` abbia `KEYCLOAK_ADMIN_PASSWORD`, `INTERNAL_SERVICE_KEY`, `LOCAL_IP` e le tre `CLOUDINARY_*` valorizzate
- [ ] `backend/cloudflared/` contenga sia `config.yml` sia `<UUID>.json`, e che `config.yml` abbia **entrambi** gli hostname (`keycloak.` e `api.`)
- [ ] `frontend/local.properties` abbia `BACKEND_IP=https://api.tripify.cloud` e `KEYCLOAK_IP=https://keycloak.tripify.cloud`
- [ ] il tunnel sia attivo su una sola macchina durante la demo

Poi, dalla radice del repo:

```bash
tar -czf tripify-config.tgz \
  backend/.env \
  backend/cloudflared \
  backend/keycloak-import/realm-export-4.json \
  backend/docker-compose.override.yml \
  frontend/local.properties
```

Il docente scompatta `tripify-config.tgz` dentro la sua copia del repo (`tar -xzf tripify-config.tgz`) e i file finiscono già al posto giusto; poi cambia solo `sdk.dir` in `frontend/local.properties`.
