# Hợp đồng cấp quyền Identity cho Progress

Progress cần hai permission tên thuần trong access JWT: `progress:read` và `progress:update`. Đề xuất cấp cho ROLE_STUDENT, ROLE_TEACHER và ROLE_ADMIN cho dữ liệu cá nhân; không mặc định cấp ROLE_GUEST. ADMIN cũng cần permission cụ thể. Danh tính chủ tài khoản vẫn lấy từ JWT sub.

Permission catalog, liên kết role_permissions và migration thuộc repo dts-identity. Đây là phụ thuộc triển khai, không phải schema của Progress. Trước khi sửa Identity phải xác định nhánh, kiểm source/migration hiện tại và chọn version kế tiếp; không tự áp dụng SQL vào database đang dùng từ gói Progress.

File SQL ở identity-handoff của gói ZIP là bản bàn giao đề xuất theo Identity V1–V10 đã đọc. Tên V11 chỉ dùng nếu repo Identity vẫn dừng ở V10. Cần review/apply bằng Flyway trên đúng repo/nhánh Identity; giữ nguyên migration cũ. Gói áp dụng Progress không chép hoặc chạy SQL này.

Sau migration, đăng nhập/refresh để Identity cấp access JWT mới có permissions. Token đã cấp từ trước không tự có quyền mới. Kiểm tài khoản đang học và admin trước khi restart source Progress mới, tránh các màn hình cũ nhận 403 do token thiếu quyền.

Nếu nhóm đã chuyển xác thực về Gateway trong source local chưa push, cần đối chiếu hợp đồng truyền principal/authority đã xác thực trước khi tích hợp. Không nhận X-User-* do client tự đặt như nguồn xác thực.
