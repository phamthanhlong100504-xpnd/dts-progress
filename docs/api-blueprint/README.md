# Progress API blueprints

Theo source local ngày18/10/2026, nhánh feature/progress-backend-completion, base983f537 bằng main lúc review. Endpoint Progress dùng origin trực tiếp :8083 hoặc Gateway :8888 theo route đã cấu hình.

| Method | Path | Thành công / tài liệu |
| --- | --- | --- |
| GET | /api/v1/progress/learning-overview?licenseClass=A1 | 200, [learning_overview.md](learning_overview.md) |
| POST | /api/v1/progress/sessions | 201, [canonical_exam_progress.md](canonical_exam_progress.md) |
| POST internal | /internal/v1/practice/exam-results | 200, [canonical_exam_progress.md](canonical_exam_progress.md) |
| POST internal | /internal/v1/practice/question-progress | 200, [question_progress_ingress.md](question_progress_ingress.md) |
| GET | /api/v1/progress/dashboard | 200, [dashboard_history_streak.md](dashboard_history_streak.md) |
| GET | /api/v1/progress/streaks | 200, cùng trang |
| GET | /api/v1/progress/history | 200, cùng trang |
| GET | /api/v1/progress/recent?limit=10 | 200, cùng trang |
| GET / PATCH | /api/v1/progress/chapters | 200, [chapters_preferences.md](chapters_preferences.md) |
| PUT | /api/v1/progress/preferences/learning-program | 200 rỗng, cùng trang |

API internal dành cho worker Practice, không phải frontend route. Auth/error/ownership xem [contract_notes.md](contract_notes.md). [Local setup](../LOCAL_SETUP.md), [nghiệp vụ](../PROGRESS_FEATURES.md), [SQL V1–V3](../db/MIGRATION_CATALOG.md).

## Authorization và blueprint từng API

| Method | Path | Authorization | Blueprint |
| --- | --- | --- | --- |
| GET | /api/v1/progress/dashboard | progress:read | [Get Learning Dashboard](get_dashboard.md) |
| GET | /api/v1/progress/chapters | progress:read | [List Chapter Progress](list_chapter_progress.md) |
| PATCH | /api/v1/progress/chapters | progress:update | [Update Chapter Progress](update_chapter_progress.md) |
| GET | /api/v1/progress/streaks | progress:read | [Get Study Streak](get_streak.md) |
| POST | /api/v1/progress/sessions | progress:update | [Log Study Session](log_study_session.md) |
| GET | /api/v1/progress/history | progress:read | [List Study Session History](list_session_history.md) |
| GET | /api/v1/progress/recent | progress:read | [List Recent Study Sessions](list_recent_sessions.md) |
| GET | /api/v1/progress/learning-overview | progress:read | [Get Learning Overview](get_learning_overview.md) |
| PUT | /api/v1/progress/preferences/learning-program | progress:update | [Update Learning Program Preference](update_learning_preference.md) |
| POST | /internal/v1/practice/exam-results | X-DTS-Internal-Key | [Receive Practice Exam Progress](receive_exam_progress.md) |
| POST | /internal/v1/practice/question-progress | X-DTS-Internal-Key | [Receive Practice Question Progress](receive_question_progress.md) |
| GET | / | Public | [Get Progress Service Home](get_service_home.md) |

Xem [ma trận và rollout](authorization.md).
