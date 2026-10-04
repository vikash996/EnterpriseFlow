
# EnterpriseFlow system design

The platform is a Next.js client backed by a stateless Spring Boot REST API. PostgreSQL is the system of record; Flyway migrations own its schema. JWT authentication identifies the user, while controller and service checks enforce administrator, project owner, and member boundaries.

The current product modules are users/RBAC, projects, tasks/comments, activity logs, notifications, document knowledge lookup, meeting transcript summaries, and dashboard statistics. Document files are stored outside the database with searchable metadata and extracted text retained in PostgreSQL. The local knowledge endpoint returns only source-backed excerpts; an external LLM can be added behind the backend when a provider is configured.
