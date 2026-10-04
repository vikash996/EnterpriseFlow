# Frontend authentication setup

Set `NEXT_PUBLIC_API_URL` in `Frontend/.env.local` to the Spring Boot backend origin, for example `http://localhost:8080`. This variable is intentionally public and must never contain a password, JWT secret, or other credential.

The backend currently returns the JWT in the login JSON response. For this development stage, the frontend stores the token and safe user profile in browser `localStorage` so browser requests can send a bearer token. This is vulnerable to token theft if an XSS vulnerability is introduced; a future production deployment should move to secure, `HttpOnly`, `SameSite` cookies with a backend token-refresh design.
