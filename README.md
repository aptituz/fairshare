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
