<!--
Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
SPDX-License-Identifier: GPL-3.0-only
-->

# Uberspace Deployment

This guide assumes:
- backend jar is deployed via `~/fairshare/current/backend.jar`
- frontend assets are served via `~/html/fairshare -> ~/fairshare/current/frontend`
- backend is managed via `supervisord`

The deploy workflow uploads each build into `~/fairshare/releases/<release-id>/`, switches the
`current` symlink, and rolls back to the previous release automatically if the health check fails.

## One-time setup on Uberspace

1) Create folders
```sh
mkdir -p ~/fairshare/releases ~/fairshare/logs ~/fairshare/scripts ~/html
```

2) Configure the backend service

The GitHub Actions workflow writes `~/etc/services.d/fairshare-backend.ini` on each deploy
using repository secrets. Set these secrets first:
- `FAIRSHARE_BACKEND_PORT`
- `FAIRSHARE_DB_URL`
- `FAIRSHARE_DB_USERNAME`
- `FAIRSHARE_DB_PASSWORD`
- `FAIRSHARE_JWT_SECRET`
- `FAIRSHARE_CORS_ALLOWED_ORIGINS`
- `FAIRSHARE_HEALTHCHECK_URL` (for example `https://YOUR.DOMAIN/fairshare/api/auth/status`)

3) Configure the web backend proxy
```sh
uberspace web backend set /fairshare/api --http --port <PORT>
```

4) No manual SPA file copy is required

The deploy workflow writes the frontend `.htaccess` file into each uploaded release before it
switches `~/html/fairshare` to the new frontend build.

## Required application settings

Backend `application.yml` supports env overrides:
- `PORT` (listening port)
- `FAIRSHARE_CONTEXT_PATH` (default `/fairshare`)
- `FAIRSHARE_CORS_ALLOWED_ORIGINS` (comma-separated)
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`, `JWT_EXPIRATION_MINUTES`

Frontend build:
- `VITE_API_BASE_URL=/fairshare` to reach `/fairshare/api/*`

## Automatic rollback

On every deploy, the workflow:

1. uploads backend and frontend artifacts into a new release directory
2. points `~/fairshare/current` and `~/html/fairshare` to the new release
3. restarts `fairshare-backend`
4. polls `FAIRSHARE_HEALTHCHECK_URL`
5. rolls back to the previous release and fails the workflow if the health check never succeeds

The workflow should use a public, unauthenticated URL that verifies the app is really usable.
`/fairshare/api/auth/status` is the recommended default because it exercises routing, security,
application startup, and database access.

## GitHub Actions secrets

Set these repository secrets:
- `UBERSPACE_HOST`
- `UBERSPACE_USER`
- `SSH_PRIVATE_KEY`
- `FAIRSHARE_BACKEND_PORT`
- `FAIRSHARE_DB_URL`
- `FAIRSHARE_DB_USERNAME`
- `FAIRSHARE_DB_PASSWORD`
- `FAIRSHARE_JWT_SECRET`
- `FAIRSHARE_CORS_ALLOWED_ORIGINS`
- `FAIRSHARE_HEALTHCHECK_URL`
