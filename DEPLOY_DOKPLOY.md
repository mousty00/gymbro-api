# Deploy — Dokploy

Stack: Spring Boot API (Dockerfile) · PostgreSQL (Dokploy Database) · S3 · SMTP.
Traefik (managed by Dokploy) terminates TLS; the API trusts its `X-Forwarded-*` headers via
`server.forward-headers-strategy=native`.

## 1. DNS

A record `api.<domain>` → VPS IP (plus `api-staging.<domain>` if you want staging).

## 2. Postgres

**Project → New Service → Database → PostgreSQL**

| Field    | Value                   |
|----------|-------------------------|
| Name     | `gymbro-postgres`       |
| Database | `gymbro`                |
| User     | `gymbro`                |
| Password | `openssl rand -hex 20`  |

No external port. The API reaches it on `dokploy-network` by the service's internal host
(shown in Dokploy → Database → Internal Credentials).

Flyway runs `db/migration` on boot (`baseline-on-migrate=true`, so an existing DB created from
`sql/gymbro.sql` is adopted at V1 and only V2+ runs).

## 3. API

**New Service → Application**

| Field      | Value                    |
|------------|--------------------------|
| Name       | `gymbro-api`             |
| Source     | GitHub repo `gymbro-api` |
| Branch     | `main`                   |
| Build type | Dockerfile               |

**Domain:** `api.<domain>` → container port `8080`, HTTPS on (Let's Encrypt).

**Environment** — copy `.env.prod.example`, fill in:

```env
SPRING_PROFILES_ACTIVE=prod
JAVA_TOOL_OPTIONS=-Xms256m -Xmx512m

SPRING_DATASOURCE_URL=jdbc:postgresql://<internal-host>:5432/gymbro
SPRING_DATASOURCE_USERNAME=gymbro
SPRING_DATASOURCE_PASSWORD=<postgres password>

JWT_SECRET=<openssl rand -base64 64>   # never change after first deploy: logs everyone out
APP_CORS_ALLOWED_ORIGINS=https://app.<domain>

# + SMTP, AWS S3 and multipart vars from .env.prod.example
```

Missing `JWT_SECRET`, `APP_CORS_ALLOWED_ORIGINS`, datasource, S3 or mail vars → app fails at
startup (intended: no silent insecure defaults).

## 4. Health

`GET https://api.<domain>/api/actuator/health` → `{"status":"UP"}`.
The Docker image has a `HEALTHCHECK` on the same endpoint; Dokploy shows it as container health.

## 5. Auto-deploy

Application → **General** → enable **Auto Deploy**. Push to `main` → redeploy.
CI (`.github/workflows/ci.yml`) runs `mvn verify` against Postgres on every push/PR.

## 6. Backups

Database → **Backups** → add a destination (S3/R2) and a daily schedule. Test a restore once.

## Local prod-like run

```bash
docker compose up --build   # uses app.env + database.env, prod profile
```
