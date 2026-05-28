# TrainSmart Backend

Spring Boot 3.2 REST API for the TrainSmart cycling training app. Generates AI-powered training plans, tracks session logs, and syncs with Strava.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/plans` | Generate a new training plan |
| GET | `/api/plans/{id}` | Get plan with logs |
| POST | `/api/plans/{id}/logs` | Save session log + AI review |
| POST | `/api/plans/{id}/share` | Generate share code |
| GET | `/api/plans/share/{code}` | Get shared plan (read-only) |
| GET | `/api/auth/strava` | Start Strava OAuth |
| GET | `/api/auth/strava/callback` | Strava OAuth callback |
| GET | `/api/strava/activities/{planId}` | Sync Strava activities |
| POST | `/api/strava/webhook` | Strava push webhook |
| GET | `/api/strava/webhook` | Strava webhook verification |
| GET | `/api/health` | Health check |

## Environment Variables

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | PostgreSQL connection URL |
| `ANTHROPIC_API_KEY` | Anthropic API key |
| `STRAVA_CLIENT_ID` | Strava app client ID |
| `STRAVA_CLIENT_SECRET` | Strava app client secret |
| `STRAVA_REDIRECT_URI` | OAuth callback URL |
| `STRAVA_WEBHOOK_VERIFY_TOKEN` | Random token for webhook verification |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins (e.g. frontend URL) |
| `FRONTEND_URL` | Frontend URL for redirects after Strava auth |

## Local Development

```bash
mvn spring-boot:run
```

Requires a PostgreSQL database. Set `DATABASE_URL` in your environment or create `src/main/resources/application-local.properties`.

## Docker

```bash
docker build -t trainsmart-backend .
docker run -p 8080:8080 -e DATABASE_URL=... -e ANTHROPIC_API_KEY=... trainsmart-backend
```
