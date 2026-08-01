# DTS Progress Service — API Documentation

> **Base URL:** `http://localhost:8083`
> **Auth:** Bearer JWT (issued by identity-service)
> **Swagger UI:** `http://localhost:8083/swagger-ui.html`

---

## Authentication

All endpoints require a valid JWT access token in the `Authorization` header:

```
Authorization: Bearer <accessToken>
```

Get a token from identity-service (`POST /api/v1/auth/login`).

---

## Endpoints

### Home

```
GET /
```

Returns service info (public, no auth required).

### Dashboard

```
GET /api/v1/progress/dashboard
```

Returns overall learning statistics:

| Field | Type | Description |
|---|---|---|
| `totalExams` | int | Total exam sessions completed |
| `totalPracticeSessions` | int | Total practice sessions |
| `totalStudyTimeSeconds` | long | Total time spent studying |
| `totalQuestionsAnswered` | int | Questions answered across all sessions |
| `totalCorrectAnswers` | int | Correct answers across all sessions |
| `averageScore` | double | Average score percentage |
| `examsPassed` | int | Number of passed exams |
| `examsFailed` | int | Number of failed exams |
| `passRate` | double | Pass rate percentage |
| `currentStreak` | int | Current consecutive study days |
| `longestStreak` | int | Longest streak ever |
| `lastStudyDate` | date | Last day the user studied |
| `chaptersTotal` | int | Total chapters tracked |
| `chaptersCompleted` | int | Chapters fully completed |
| `chaptersInProgress` | int | Chapters in progress |
| `chaptersProgressPercent` | double | Completion percentage |

### Chapter Progress

```
GET  /api/v1/progress/chapters      — List all chapter progress
PATCH /api/v1/progress/chapters     — Update chapter progress
```

**PATCH request body:**

```json
{
  "chapterId": 1,
  "chapterName": "Khái niệm và quy tắc",
  "questionsTotal": 25,
  "correctCount": 10
}
```

### Streaks

```
GET /api/v1/progress/streaks
```

Returns current streak, longest streak, last study date, and whether user studied today.

### Study Sessions

```
POST /api/v1/progress/sessions       — Log a study/exam session
GET  /api/v1/progress/history        — Paginated history
GET  /api/v1/progress/recent?limit=10 — Recent sessions
```

**POST request body:**

```json
{
  "sessionType": "EXAM",
  "examType": "A1",
  "mode": "EXAM",
  "examId": null,
  "questionsCount": 25,
  "correctCount": 21,
  "wrongCount": 4,
  "durationSeconds": 1200
}
```

---

## Kafka Events Consumed

| Topic | Event Type | Action |
|---|---|---|
| `user-events` | `USER_CREATED` | Create `UserProgress` record |
| `user-events` | `USER_UPDATED` | Update username in `UserProgress` |
| `user-events` | `USER_DELETED` | Delete `UserProgress` record |

---

## Database

- **Database:** `dts_progress`
- **Port:** `5434` (external), `5432` (internal)
- **Migrations:** Flyway (`src/main/resources/db/migration/`)

### Tables

| Table | Description |
|---|---|
| `user_progress` | Per-user overall statistics and streaks |
| `chapter_progress` | Per-user per-chapter learning progress |
| `study_sessions` | Individual study/exam session logs |

---

## Running

### Local dev:

```bash
docker compose -f docker-compose.infra.yml up -d
mvn spring-boot:run
```

### Full Docker:

```bash
docker compose up -d
```

---

## Port Allocation

| Service | Port |
|---|---|
| identity-service | 8081 |
| practice-service | 8082 |
| **progress-service** | **8083** |
