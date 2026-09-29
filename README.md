# Fairshare

Fairshare is a simple budget planner example project developed with genAI. It lets users define
recurring income and expense budget items, assign them to categories, and view a monthly budget
summary.

## Tech stack

- Backend: Kotlin + Spring Boot + SQLite (Liquibase for migrations)
- Frontend: Vue.js
- Docker Compose for running the backend

## Run for testing

### Backend + frontend (Docker Compose)

```bash
docker compose up --build
```

The API will be available at `http://localhost:8080`, and Springdoc UI at
`http://localhost:8080/swagger-ui`. The frontend dev server will be available at
`http://localhost:5173`.

### Frontend (local dev server)

```bash
cd frontend
npm install
npm run dev
```

Vite will print the local URL, typically `http://localhost:5173`.

### Quick API check

```bash
curl http://localhost:8080/api/budget/monthly-summary
```

## Investigating failed API requests

The backend returns a generated `X-Request-ID` header and logs failed requests at
WARN, including method, path (without query parameters), status, duration,
access-token authentication outcome and whether a non-empty refresh cookie was
present. The diagnostics filter runs before Spring Security, so it also records
security rejections. It does not log credentials, cookie values, bodies or JWT claims.

1. In browser developer tools, enable Preserve log in Network and Console and
   reproduce the failure. Console diagnostics distinguish failed initial requests,
   refresh requests and retries. Final errors include the endpoint and request ID
   when available. An initial 401 followed by successful refresh and retry can be
   normal access-token expiry.
2. Search backend logs for the failed response's `X-Request-ID` (for Docker Compose,
   use `docker compose logs backend`). Authentication outcomes distinguish
   `missing_header`, `unsupported_scheme`, `invalid_token`, `unknown_user` and
   `stale_token_version`. The last means the JWT's version differs from the person's
   current version, for example after password changes revoke sessions.
   `authenticated` means authentication succeeded, not that the request succeeded.
   `not_evaluated` means the JWT filter did not evaluate the request, for example
   because an earlier filter rejected it.
3. For additional detail, set these backend environment variables and restart:

   ```text
   FAIRSHARE_HTTP_LOG_LEVEL=DEBUG
   FAIRSHARE_JWT_LOG_LEVEL=DEBUG
   ```

   This records successful requests and JWT failure types such as
   `ExpiredJwtException`, `SignatureException` or `missing_token_version`, correlated
   by request ID. Restore normal levels after collecting a reproduction.
4. Inspect `/api/auth/refresh` alongside the original failure. A 401 with
   `refreshCookiePresent=false` points to a missing cookie; inspect browser cookie
   blocking, domain/path, HTTPS and SameSite behavior. Presence alone does not mean
   the refresh token is valid. Across backend replicas, verify consistent
   `JWT_SECRET`, synchronized clocks and the same refresh-token database.
5. If a response has no request ID, check reverse-proxy/ingress access and
   authentication logs, routing for `/fairshare/api/`, and the deployed backend
   version. A missing ID is a clue, not proof the request missed the backend:
   proxies can strip headers. Compare the raw Network response headers, since
   cross-origin JavaScript also needs CORS to expose the header. Retain
   `X-Request-ID` response headers at the proxy, and correlate proxy timestamps,
   paths and upstream status with backend logs.

These diagnostics preserve authentication and retry behavior. They help locate
the failing layer; partial data and a 401 alone do not establish the root cause.
