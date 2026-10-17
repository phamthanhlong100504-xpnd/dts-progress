# DTS Progress — API reference

API hiện hành được trình bày tại [docs/api-blueprint/README.md](docs/api-blueprint/README.md).

- [Learning overview](docs/api-blueprint/learning_overview.md): số liệu theo hạng và ngân hàng hiện tại của Practice.
- [Canonical exam progress](docs/api-blueprint/canonical_exam_progress.md): ingress nội bộ và POST sessions có examId.
- [Question progress ingress](docs/api-blueprint/question_progress_ingress.md): inbox, event sequence và projection theo câu.
- [Dashboard, history, streak](docs/api-blueprint/dashboard_history_streak.md): các endpoint đã có, giá trị nullable và phạm vi thống kê.
- [Chapter và preference](docs/api-blueprint/chapters_preferences.md): accumulator cũ và learning program Redis.
- [Contract notes](docs/api-blueprint/contract_notes.md): xác thực, response/lỗi và ranh giới với Result/Practice.

[Cách chạy local](docs/LOCAL_SETUP.md), [nghiệp vụ](docs/PROGRESS_FEATURES.md), [migration](docs/db/MIGRATION_CATALOG.md). Đây là tài liệu source hiện có, không phải báo cáo đã thực hiện API/test runtime trong bước chuẩn bị Git.
