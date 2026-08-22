# Veneranda University — Backend

Quarkus API for the LMS. Deploy on **Render (Docker)** with **Supabase PostgreSQL**.

## Render setup — Docker (recommended)

Render builds and runs the included **multi-stage Dockerfile** (Maven build → JRE runtime). No native build pack required.

1. Push this repo to GitHub as `veneranda-university-backend`.
2. [Render Dashboard](https://dashboard.render.com/) → **New → Blueprint** (easiest — uses `render.yaml`).
   - Or **New → Web Service** → connect repo → set **Language: Docker**.
3. Confirm these settings:
   - **Runtime:** Docker
   - **Dockerfile path:** `./Dockerfile`
   - **Docker context:** `.` (repo root)
   - **Health check path:** `/q/health`
4. Set environment variables:

| Variable | Value |
|----------|--------|
| `QUARKUS_PROFILE` | `prod` |
| `DB_HOST` | Supabase → Settings → Database → Host |
| `DB_PORT` | `5432` |
| `DB_NAME` | `postgres` |
| `QUARKUS_DATASOURCE_USERNAME` | Supabase user (often `postgres`) |
| `QUARKUS_DATASOURCE_PASSWORD` | Supabase database password |
| `JWT_SECRET` | Long random string |
| `GOOGLE_CLIENT_ID` | Same as frontend |
| `CORS_ORIGINS` | `https://<username>.github.io` |
| `REDIS_URL` | *(optional)* Upstash `rediss://...` URL |

5. Deploy. Flyway runs migrations on startup (`V1`–`V25`).

Default admin (from seed migration): `admin` / `Admin@123` — **change after first login in production.**

## Supabase setup

1. [supabase.com](https://supabase.com) → New project (free tier).
2. **Project Settings → Database** → copy host, user, password.
3. Use **direct connection** (port 5432) for Render (long-running server).

## Redis (optional)

The app does not require Redis for core features. Health checks ignore Redis.

For future caching, create a free [Upstash Redis](https://upstash.com/) database and set `REDIS_URL` on Render.

## Local dev

Copy `.env.example` to `.env` and fill values, or export variables, then:

```bash
mvn quarkus:dev
```

API: http://localhost:8081

## Deploy workflow

```
git push origin main  →  Render rebuilds Docker image  →  Flyway migrates Supabase
```

Each push to `main` triggers a fresh Docker build on Render. Flyway runs inside the container on startup.

Add new SQL files under `src/main/resources/db/migration/V26__....sql` for schema changes.
