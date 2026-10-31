# API Blueprint: Dashboard, session history và streak

Các endpoint dưới /api/v1/progress yêu cầu Authorization Bearer, chủ thể từ JWT. Qua Gateway direct path giữ nguyên khi PROGRESS_SERVICE_URL đúng. Không có query userId để chọn người khác.

## GET /dashboard

200 ApiResponse<DashboardResponse>. data gồm:

- totalExams, totalPracticeSessions, totalStudyTimeSeconds, totalQuestionsAnswered, totalCorrectAnswers, averageScore.
- examsPassed, examsFailed, passRate.
- currentStreak, longestStreak, lastStudyDate.
- chaptersTotal, chaptersCompleted, chaptersInProgress, chaptersProgressPercent.

Tổng ban đầu lấy user_progress; thiếu row thì có nhánh getOrCreateUserProgress. averageScore nullable. Pass/fail chỉ đếm sessionType EXAM, status COMPLETED, passed true/khác true; TIMEOUT không tham gia phép đếm này. Không có completed EXAM thì passRate=null, không ép0. Chapter summary lấy chapter_progress legacy, không thay bằng learning-overview theo license/bankVersion.

## GET /streaks

200 ApiResponse<StreakResponse>: currentStreak, longestStreak, lastStudyDate, studiedToday. studiedToday dùng lastStudyDate so LocalDate.now của process. Canonical service cập nhật ngày bằng Asia/Ho_Chi_Minh; nhánh legacy recordStudy dùng ngày process, vì vậy không khẳng định tất cả cách ghi cùng timezone nếu môi trường khác nhau.

## GET /history

200 ApiResponse<Page<SessionHistoryResponse>>. Spring Pageable nhận page/size/sort; repository tìm đúng user, order startedAt desc theo method. Không có các filter activityType/licenseClass/date range của Result history trong controller Progress này.

Item gồm id, examId, sessionType, examType, mode, status, questionsCount, correctCount, wrongCount, score, passed, durationSeconds, startedAt, completedAt. id là ID study_sessions của Progress; examId nullable và là ID nguồn Practice khi có, không phải Result resultId. Không dùng item Progress thay contract result detail/source routing của Result.

## GET /recent?limit=10

200 ApiResponse<List<SessionHistoryResponse>>. Mặc định limit10; service lấy các row user có startedAt sau Instant.EPOCH, order startedAt desc rồi stream.limit(limit). Controller không có validation @Min/@Max riêng cho limit; không tự mô tả giới hạn100 hoặc lỗi400 chuẩn nếu gửi số âm. Client gửi giá trị dương hợp lý theo nhu cầu UI.

## Phạm vi phiên canonical/legacy

Phiên có examId được lấy từ Practice receipt hoặc ingress, upsert theo V2. Phiên không examId giữ cách ghi legacy. averageScore, passRate, số câu đã học projection có phạm vi khác nhau; dùng đúng tên/nguồn server, không recompute một KPI bằng nguồn của KPI khác.

Nguồn: [ProgressController](../../src/main/java/com/dts/progress/controller/ProgressController.java), [ProgressService](../../src/main/java/com/dts/progress/service/ProgressService.java), [Dashboard DTO](../../src/main/java/com/dts/progress/dto/response/DashboardResponse.java), [Session DTO](../../src/main/java/com/dts/progress/dto/response/SessionHistoryResponse.java).

## Authorization cập nhật ngày 31 tháng 10 năm 2026

Các API người dùng trong trang này phải có quyền tương ứng trong [ma trận authorization](authorization.md): GET dùng `progress:read`; PATCH chapters, POST sessions và PUT preference dùng `progress:update`. Hai POST ingress vẫn dùng shared key riêng, không dùng permission học viên. Giữ ownership, DTO và nghiệp vụ đã mô tả. Blueprint từng endpoint trong [mục lục](README.md) bổ sung Part 0–4.
