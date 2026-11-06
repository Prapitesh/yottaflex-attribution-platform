# Code structure and conventions

## Frontend (React + TypeScript)

- `src/pages`: route-level screens and page orchestration (currently `DashboardPage.tsx`).
- `src/components`: reusable presentation components with typed props (for example, `MetricCard` and `ModelOption`).
- `src/services`: API client and HTTP endpoint functions. Components should call service functions rather than constructing URLs directly.
- `src/App.tsx`: application composition only.
- `src/main.tsx`: React bootstrap and global stylesheet import.

Use TypeScript for all UI code, explicit prop interfaces for reusable components, named exports for shared components, and keep API concerns out of visual components. Keep the existing Vite build command as the frontend quality gate: `npm run build`.

## Backend (Java 21 + Spring Boot)

Base package: `ai.yottaflex.attribution`.

- `config`: framework and application configuration.
- `controller`: HTTP routing, request binding and response delegation.
- `dto`: validated request/response contracts; do not expose persistence entities as API contracts.
- `entity`: JPA persistence mappings only.
- `repository`: Spring Data persistence access.
- `service`: business rules, orchestration, and reporting.
- `generator`: deterministic synthetic dataset creation.

Use constructor injection, keep controllers thin, keep business rules in services, validate external inputs at the API boundary, use parameterized SQL, and use Flyway for schema changes. Hibernate runs with `ddl-auto: validate`; do not use automatic schema creation in application runtime. Preserve dataset runs and attribution history instead of deleting prior generated records.

Run backend checks with `mvn test`. Run frontend checks with `npm run build`.

## PostgreSQL target

Use PostgreSQL 18 at `localhost:5432`, database `yottaflex_attribution`. Supply credentials through environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) or an ignored local environment file. Never commit real credentials.
