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
