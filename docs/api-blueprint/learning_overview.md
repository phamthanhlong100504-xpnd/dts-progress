# API Blueprint: Learning overview

## Endpoint / request

GET /api/v1/progress/learning-overview?licenseClass=A1. Header Authorization: Bearer <access-token>. Qua Gateway dùng cùng direct path này hoặc /api/progress/v1/progress/learning-overview?licenseClass=A1. Không có body hoặc tham số chọn userId; user lấy từ JWT.

licenseClass bắt buộc; service trim/uppercase, Practice xác nhận hạng trong catalog. Không tự lấy hạng mặc định khi thiếu query. Cần schema V3 và Practice có endpoint GET /api/v1/questions/learning-catalog.

## Logic

1. Forward Bearer và license tới Practice, đọc licenseClass/bankVersion/totalQuestions/criticalQuestions/chapters.
2. Kiểm tra response hợp lệ: success/data, license khớp, bankVersion không trắng, totalQuestions>0 và chapters không null.
3. Đọc user_question_progress của đúng user/license/bankVersion, GROUP BY chapterId.
4. learned COUNT theo chapter_learned OR critical_learned; review COUNT needs_review; critical COUNT is_critical AND learned; lastStudiedAt là MAX(last_studied_at).
5. Ghép chapter catalog có totalQuestions>0. Thiếu tên chương hoặc count vượt total/critical canonical →409.
6. Tổng hợp overall/review/critical, integer completionPercent và status; không ghi dữ liệu trong luồng overview.

## Response — 200

Ví dụ số liệu minh họa shape, không phải catalog thật của A1 hoặc dữ liệu seed:

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "licenseClass": "A1",
    "bankVersion": "example-bank",
    "overall": {"totalQuestions": 10, "learnedQuestions": 0, "completionPercent": 0, "status": "NOT_STARTED"},
    "review": {"needReviewCount": 0},
    "critical": {"totalQuestions": 2, "criticalStudiedQuestions": 0, "completionPercent": 0, "status": "NOT_STARTED"},
    "chapters": [
      {"chapterId": 1, "chapterName": "Chương minh họa", "totalQuestions": 10, "learnedQuestions": 0, "completionPercent": 0, "status": "NOT_STARTED", "needReviewCount": 0, "criticalStudiedQuestions": 0, "lastStudiedAt": null}
    ]
  },
  "timestamp": "2026-10-18T00:00:00Z"
}
```

Percent=Math.round(100f*count/total), total0 trả0. Status NOT_STARTED khi count0 hoặc total0, COMPLETED khi count=total, còn lại IN_PROGRESS. Frontend dùng đúng metadata/tổng/phần trăm từ response; không gộp projection khác bankVersion hoặc giả định bằng legacy /chapters.

## Lỗi

- 400 BAD_REQUEST: license trắng hoặc Practice trả400.
- 401 từ security khi thiếu/sai Bearer; 403 FORBIDDEN khi không forward được Bearer hoặc Practice401/403.
- 409 CONFLICT: thiếu chapter metadata hoặc progress vượt catalog.
- 502 PRACTICE_RESPONSE: envelope/catalog không hợp lệ; 502 PRACTICE_UNAVAILABLE: lỗi mạng hoặc các lỗi upstream khác được client ánh xạ.
- Query/header bắt buộc cần gửi đủ; không suy ra mọi lỗi binding có cùng errorCode với service validation.

Nguồn: [LearningOverviewController](../../src/main/java/com/dts/progress/controller/LearningOverviewController.java), [LearningOverviewService](../../src/main/java/com/dts/progress/service/LearningOverviewService.java), [PracticeLearningBankClient](../../src/main/java/com/dts/progress/service/PracticeLearningBankClient.java), [DTO](../../src/main/java/com/dts/progress/dto/response/LearningOverviewResponse.java).
