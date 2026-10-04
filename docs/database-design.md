# EnterpriseFlow Database Foundation

EnterpriseFlow uses PostgreSQL with Flyway-managed schema migrations. The backend validates the JPA schema at startup and never creates or updates tables automatically.

## Prerequisites

- PostgreSQL 16 or later
- Java 21 or later

Create a local database and user using your own secure password:

```sql
CREATE DATABASE enterpriseflow;
CREATE USER enterpriseflow_app WITH PASSWORD 'choose-a-secure-password';
GRANT ALL PRIVILEGES ON DATABASE enterpriseflow TO enterpriseflow_app;
```

Connect to `enterpriseflow` as a database owner (or grant schema privileges) before the first migration:

```sql
GRANT USAGE, CREATE ON SCHEMA public TO enterpriseflow_app;
```

## Environment configuration

Copy `Backend/.env.example` to `Backend/.env` and configure these variables in your shell, IDE, or deployment environment. The `.env` file is not loaded automatically by Spring Boot; it is a local reference file and must not be committed.

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_HOST` | `localhost` | PostgreSQL server host |
| `DB_PORT` | `5432` | PostgreSQL server port |
| `DB_NAME` | `enterpriseflow` | Database name |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | none | Database password; required when the chosen PostgreSQL user has one |

The JDBC URL is assembled as `jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}`.

## Schema migrations

Flyway runs migrations from `Backend/src/main/resources/db/migration/` when the backend starts. The initial migration, `V1__create_users_table.sql`, creates the `users` table with UUID identifiers, unique email addresses, role constraints, and audit timestamps. `V2__create_workspace_tables.sql` adds projects and members, tasks/comments, notifications, documents, meetings, and activity logs. Foreign keys preserve ownership and project relationships; indexes cover frequent project, assignee, notification, and audit queries.

Do not alter a migration that has been applied outside local development. Add a new versioned migration instead.

## Running the backend

From `Backend` on PowerShell:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "enterpriseflow"
$env:DB_USERNAME = "enterpriseflow_app"
$env:DB_PASSWORD = "your-local-password"
.\mvnw.cmd spring-boot:run
```

The first successful startup applies pending Flyway migrations. The unauthenticated infrastructure endpoint is available at `GET /api/health` and returns `{ "status": "UP" }`.

## Maven Wrapper commands

From `Backend`:

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
.\mvnw.cmd spring-boot:run
```
