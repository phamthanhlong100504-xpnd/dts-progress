# API Blueprint: Chapter legacy và learning preference

## GET /api/v1/progress/chapters

Bearer, chủ thể JWT. 200 ApiResponse<List<ChapterProgressResponse>>, repository theo userId và chapterId tăng dần. Item có chapterId/name, status, questionsAnswered/Total, correctCount, score, completionPercent, startedAt/completedAt/lastStudiedAt.

completionPercent legacy là questionsAnswered/questionsTotal làm tròn2 số thập phân và chặn tối đa100 ở mapper; total0 trả0. Không cùng định nghĩa learned unique câu của /learning-overview. Score dựa số đúng/total, khác completionPercent.

## PATCH /api/v1/progress/chapters

Bearer, body UpdateChapterProgressRequest:

```json
{"chapterId":1,"chapterName":"Chương minh họa","questionsTotal":10,"questionsCount":2,"correctCount":1,"wrongCount":1}
```

chapterId bắt buộc (DTO không có @Min cho ID); chapterName không trắng; total/count>=1; correct>=0; wrong optional>=0. Service từ chối correct>questionsCount hoặc correct+wrong>questionsCount khi wrong có giá trị.

Tìm/tạo chapter user, cập nhật name/total, bắt đầu nếu NOT_STARTED. Cộng questionsCount vào answered và chặn ở total; cộng correct chặn ở answered; score correct/total tối đa100 làm tròn2 số. Complete khi answered>=total. Trả200 ApiResponse<ChapterProgressResponse>.

Đây là thao tác cộng dồn, không có eventId/idempotency; gửi lặp có thể ảnh hưởng tiến độ. Không dùng PATCH này để cập nhật projection QUESTION_PROGRESS_CHANGED hoặc chuyển dữ liệu chapter cũ thành overview bằng cách tự gửi lại từ frontend.

## PUT /api/v1/progress/preferences/learning-program

Bearer, body {"programCode":"A1"}, trả200 body rỗng. Controller không dùng @Valid cho DTO này, không có danh sách enum/validation GPLX như Identity. Không yêu cầu parser JSON cho response rỗng.

UserPreferenceService lưu Redis key user:<UUID>:program và publish channel:user_program_events với userId/program. Không có TTL được set ở nhánh này. Không tự ghi Identity.users.selected_license_class hoặc tạo migration. Đổi GPLX hồ sơ chuẩn thuộc /api/v1/users/me/license của Identity.

Nguồn: [ProgressController](../../src/main/java/com/dts/progress/controller/ProgressController.java), [ProgressService](../../src/main/java/com/dts/progress/service/ProgressService.java), [Preference controller](../../src/main/java/com/dts/progress/controller/UserPreferenceController.java), [Preference service](../../src/main/java/com/dts/progress/service/UserPreferenceService.java), [Mapper](../../src/main/java/com/dts/progress/mapper/ProgressMapper.java).
