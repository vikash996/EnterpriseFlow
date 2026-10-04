# API documentation

All protected routes require `Authorization: Bearer <JWT>` and return `{ success, message, data }`.

- `GET|POST /api/projects`, `GET|PUT|DELETE /api/projects/{id}`, `POST /api/projects/{id}/members/{userId}`
- `GET|POST /api/projects/{projectId}/tasks`, `PUT|DELETE /api/tasks/{id}`, `GET|POST /api/tasks/{id}/comments`
- `GET /api/dashboard`, `GET /api/analytics`, `GET /api/activity`, `GET|POST /api/notifications`, `POST /api/notifications/{id}/read`
- `POST /api/documents` (multipart), `GET /api/documents/search?q=`, `POST /api/ai/ask`
- `GET|POST /api/meetings`, `POST /api/meetings/{id}/summarize`

Existing `/api/auth/*` and `/api/users/*` endpoints remain unchanged.
