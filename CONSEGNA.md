# Nota interna del gruppo — preparare la consegna

Questo file **non è per il docente**: è il promemoria per noi su come impacchettare
la configurazione e, se serve, ricreare il tunnel. Il README è la doc per il prof.

## Preparare `tripify-config.tgz`

I file coi segreti sono gitignored. Vanno consegnati al prof **fuori da Git**
(email / piattaforma del corso) come un unico archivio.

Prima di generarlo, controlla che:

- [ ] `backend/keycloak-import/realm-export-4.json` abbia i valori **reali** (non `CHANGEME`) in `smtpServer.password` e nel `clientSecret` del provider `google`, e `"verifyEmail": true`
- [ ] `backend/.env` abbia `KEYCLOAK_ADMIN_PASSWORD`, `INTERNAL_SERVICE_KEY`, `LOCAL_IP` e le tre `CLOUDINARY_*` valorizzate
- [ ] `backend/cloudflared/` contenga sia `config.yml` sia `<UUID>.json`, e che `config.yml` abbia **entrambi** gli hostname (`keycloak.` e `api.`)
- [ ] `frontend/local.properties` abbia `BACKEND_IP=https://api.tripify.cloud` e `KEYCLOAK_IP=https://keycloak.tripify.cloud`

Poi, dalla radice del repo:

```bash
tar -czf tripify-config.tgz \
  backend/.env \
  backend/cloudflared \
  backend/keycloak-import/realm-export-4.json \
  backend/docker-compose.override.yml \
  frontend/local.properties
```

Il prof scompatta `tripify-config.tgz` dentro la sua copia del repo
(`tar -xzf tripify-config.tgz`) e i file finiscono già al posto giusto; poi
cambia solo `sdk.dir` in `frontend/local.properties`.

## Consegna

1. Invita il prof come collaboratore al repo (GitHub → Settings → Collaborators).
2. Mandagli `tripify-config.tgz` **a parte**, mai dentro il repo.
3. Durante il suo test: il tunnel Cloudflare gira da **una macchina alla volta** —
   se lo esegue lui, tieni spento il nostro stack.

## Ricreare il tunnel Cloudflare da zero

Serve un dominio su Cloudflare (piano free). Da terminale, senza dashboard né carta:

```bash
brew install cloudflared                       # o il pacchetto per il tuo OS
cloudflared tunnel login                       # scegli la zona, Authorize
cloudflared tunnel create tripify-keycloak     # annota l'UUID
cloudflared tunnel route dns tripify-keycloak keycloak.tripify.cloud
cloudflared tunnel route dns tripify-keycloak api.tripify.cloud
cp ~/.cloudflared/<UUID>.json backend/cloudflared/
```

`backend/cloudflared/config.yml`:

```yaml
tunnel: <UUID>
credentials-file: /etc/cloudflared/<UUID>.json
ingress:
  - hostname: keycloak.tripify.cloud
    service: http://keycloak:8080
  - hostname: api.tripify.cloud
    service: http://api-gateway:8080
  - service: http_status:404
```

Se si cambia dominio: aggiorna `KEYCLOAK_PUBLIC_URL` in `backend/.env`,
`KEYCLOAK_IP` / `BACKEND_IP` in `frontend/local.properties`, aggiungi il nuovo
redirect `https://<host-keycloak>/realms/tripify/broker/google/endpoint` in
Google Cloud Console e **ricompila l'app**.
