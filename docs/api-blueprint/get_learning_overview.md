# Get Learning Overview

## Part 0 — Classification & Identity

- **API Name**: Get Learning Overview
- **Module**: `progress`
- **API Type**: Authenticated user
- **Authorization**: `progress:read`
- **Related Data**: user_question_progress

## Part 1 — API Contract

- **HTTP Method**: `GET`
- **URL**: `/api/v1/progress/learning-overview`
- **Success Status**: `200`

### Headers

| Name | Required | Description |
| --- | --- | --- |
| Authorization | Có | Bearer access token hợp lệ của Identity |
| Content-Type | Có khi có body | application/json |

JWT claim `permissions` phải chứa đúng `progress:read`; authority runtime là `PERM_progress:read`. Role ADMIN/STUDENT không tự bypass permission. Chủ thể lấy từ JWT sub; userId trong query/body/header không chọn được tài khoản khác.

### Query Parameters

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| licenseClass | String | Có | Trim/uppercase; Practice xác nhận catalog hợp lệ |

### Response

`ApiResponse<LearningOverviewResponse>`. Field null của wrapper có thể bị lược; không tự thêm field hoặc thay đơn vị.

| Field | Java Type | Description |
| --- | --- | --- |
| licenseClass | String | Mã hạng GPLX; catalog Practice quyết định dữ liệu hợp lệ |
| bankVersion | String | Phiên bản ngân hàng dùng để giới hạn projection |
| overall | Overall | Tổng câu, số learned duy nhất, phần trăm và trạng thái |
| review | Review | Số câu cần ôn lại của cùng user/hạng/bank |
| critical | Critical | Cờ critical trong event; trong overview là nhóm thống kê critical theo DTO |
| chapters | List<Chapter> | Danh sách tổng hợp từng chương từ catalog và projection |

Các record lồng overall/review/critical/chapters giữ nguyên; xem [logic và response chi tiết](learning_overview.md).

### Error Codes

| Error Code | HTTP | Meaning |
| --- | --- | --- |
| Không bắt buộc có errorCode | 401 | JWT thiếu, hết hạn hoặc không xác minh được |
| AUTH-403 | 403 | Đã xác thực nhưng thiếu progress:read tại method security |

Lỗi nghiệp vụ/upstream và shape lỗi giữ theo [contract notes](contract_notes.md). Không hứa mọi lỗi binding có cùng mã; method security chạy khi gọi controller sau bước bind argument.

## Part 2 — Processing Specification

1. **Security Layer**: Giữ xác minh JWT/issuer hiện có, chuẩn hóa role một lần và map permission thành `PERM_<permission>`.
2. **Controller Layer**: Kiểm `hasAuthority('PERM_progress:read')` trước gọi service; lấy userId từ principal.
3. **Service Layer**: `LearningOverviewService.get`. Forward Bearer và licenseClass tới Practice lấy catalog. Đọc projection đúng user/license/bankVersion, đếm learned theo chapter_learned OR critical_learned, ghép metadata chương và trả lỗi nếu số lượng vượt catalog. Không thay công thức projection.
4. **Repository / External Interaction**: giữ truy vấn, transaction, nguồn dữ liệu và giao tiếp hiện có.

## Part 3 — Data Interaction

- **Target Data**: user_question_progress
- **Ownership**: Truy vấn/ghi theo userId từ JWT; giữ scope hạng/bank/chương của nghiệp vụ.
- **Denied Request**: không gọi service nghiệp vụ nên không ghi DB/Redis hoặc thay tiến độ.

## Part 4 — Operational Notes

- Source baseline: `f775eaa8a9a4be14d550b1ff1e7831326888f355`; target branch `fix/progress-api-authorization`.
- Bản vá chỉ bổ sung authorization và tài liệu; không thay API request/response, scoring, schema Progress hoặc worker.
- Trước rollout, Identity phải cấp quyền bằng migration và client lấy access token mới. Không tự tạo quyền trong Progress hoặc sửa JWT phía frontend.
- Không ghi JWT/key vào log hoặc tài liệu. Xem [ma trận quyền](authorization.md).

Nghiệp vụ chi tiết: [learning_overview.md](learning_overview.md).
