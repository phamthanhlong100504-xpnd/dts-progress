# Get Progress Service Home

## Part 0 — Classification & Identity

- **API Name**: Get Progress Service Home
- **Module**: `progress`
- **API Type**: Public service information
- **Authorization**: `Public`
- **Related Data**: Không có

## Part 1 — API Contract

- **HTTP Method**: `GET`
- **URL**: `/`
- **Success Status**: `200`

### Response

200 `ApiResponse` với data gồm `service`, `version`, `status`.

## Part 2 — Processing Specification

1. **Security Layer**: Cho phép truy cập endpoint home.
2. **Controller Layer**: Trả thông tin service.
3. **Service Layer**: `HomeController.home`. Trả thông tin service/version/status, không gọi các service nghiệp vụ hoặc chọn tài khoản.
4. **Repository / External Interaction**: giữ truy vấn, transaction, nguồn dữ liệu và giao tiếp hiện có.

## Part 3 — Data Interaction

- **Target Data**: Không có
- **Ownership**: Không có dữ liệu cá nhân.
- **Denied Request**: không gọi service nghiệp vụ nên không ghi DB/Redis hoặc thay tiến độ.

## Part 4 — Operational Notes

- Source baseline: `f775eaa8a9a4be14d550b1ff1e7831326888f355`; target branch `fix/progress-api-authorization`.
- Bản vá chỉ bổ sung authorization và tài liệu; không thay API request/response, scoring, schema Progress hoặc worker.
- Giữ contract truy cập hiện có của endpoint.
- Không ghi JWT/key vào log hoặc tài liệu. Xem [ma trận quyền](authorization.md).
