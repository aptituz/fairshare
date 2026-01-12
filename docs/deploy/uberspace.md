<!--
Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
SPDX-License-Identifier: GPL-3.0-only
-->

# Uberspace Deployment

This guide assumes:
- backend jar is deployed to `~/fairshare/backend.jar`
- frontend assets are deployed to `~/html/fairshare/`
- backend is managed via `supervisord`

## One-time setup on Uberspace

1) Create folders
```sh
mkdir -p ~/fairshare/logs ~/html/fairshare
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

3) Configure the web backend proxy
```sh
uberspace web backend set /fairshare/api --http --port <PORT>
```

4) Configure Apache for the SPA
```sh
cp deploy/uberspace/htaccess ~/html/fairshare/.htaccess
```

## Required application settings

Backend `application.yml` supports env overrides:
- `PORT` (listening port)
- `FAIRSHARE_CONTEXT_PATH` (default `/fairshare`)
- `FAIRSHARE_CORS_ALLOWED_ORIGINS` (comma-separated)
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`, `JWT_EXPIRATION_MINUTES`

Frontend build:
- `VITE_API_BASE_URL=/fairshare` to reach `/fairshare/api/*`

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
