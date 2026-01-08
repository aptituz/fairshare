Project: Fairshare

Description:
A simple budget planner that lets users define recurring income and expense transactions,
assign them to categories and view a monthly budget summary showing:
- total income
- expenses grouped by category
- monthly net result

Tech Stack:
- Backend: Kotlin + Spring Boot + REST API
- Database: PostgreSQL (schema managed by Liquibase)
- Frontend: Vue.js
- Docker Compose to run backend + frontend

Coding conventions:
- Do not adjust tests without asking.
- Business logic should be covered by tests.
- Each Kotlin class should have its own file.
- All Kotlin code must be ktlint-clean.
- All API endpoints must be documented with springdoc OpenAPI annotations.
- Definition of done: all API filters must use repository-level filtering (no in-memory filtering).
- Controllers should stay lean by delegating business logic to service classes.
- Organize backend code into packages by responsibility (controller, service, repo, model, dto).
- Use Liquibase for schema changes; avoid Hibernate auto-DDL.
- All calculations must happen in backend services; frontend renders results only.
- Frontend: keep `frontend/src/App.vue` minimal; put UI in view components and shared logic in composables.

Core API:
- GET/POST /api/categories
- GET/POST /api/budget-items
- POST /api/budget-items/category-correction
- POST /api/budget-items/{id}/month-override
- GET/POST /api/persons
- GET /api/budget/monthly-summary
