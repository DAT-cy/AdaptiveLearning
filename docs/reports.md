# Báo cáo học tập

## Phạm vi và giả định

Repo là nền tảng học tập, không có các bảng đơn hàng/sản phẩm/cửa hàng. Báo cáo thay thế theo schema thật: lượt thi, hiệu quả đề thi, tiến độ theo ngày và hoạt động học viên. Các API và màn hình chỉ dành cho `ROLE_ADMIN`; các vai trò hiện có không biểu diễn quản lý theo cửa hàng. Báo cáo lượt thi dùng `started_at`; trạng thái `GRADED` được xem là hoàn tất/chấm xong. Điểm trung bình tính trên điểm khác null. Tỷ lệ đạt chỉ tính trong số lượt `GRADED`, so sánh điểm với `exams.passing_score`.

Thời gian nhận ngày lịch Việt Nam, chuyển thành `LocalDateTime` đầu ngày tại `Asia/Ho_Chi_Minh`, truy vấn theo khoảng nửa mở `[from, to + 1 ngày)`. Hệ thống lưu `LocalDateTime` không offset nên cấu hình MySQL hiện tại cần nhất quán với giờ nghiệp vụ; cần migration sang `Instant`/UTC nếu dữ liệu cũ đang lưu UTC.

## Kế hoạch truy vấn / API / màn hình

| Báo cáo | Tổng hợp DB | API | Hiển thị |
|---|---|---|---|
| Hoạt động theo thời gian | `COUNT`, `AVG`, `SUM CASE` group theo ngày | `GET /api/v1/reports?from=&to=` | KPI, biểu đồ cột theo ngày |
| Hiệu quả đề thi | group `exam_id`, đếm lượt, điểm TB, lượt đạt | Cùng endpoint | Bảng đề thi, lượt, điểm, tỷ lệ đạt |
| Tiến độ học viên | group `user_id`, đếm lượt, điểm TB, hoạt động mới nhất | Cùng endpoint | Bảng tìm kiếm, phân trang, drill-down bằng lọc học viên |
| Export | Dùng cùng bộ lọc và kết quả aggregate | `GET /api/v1/reports/export.xlsx|pdf?from=&to=` | XLSX và PDF tải xuống |

Các ngày không phát sinh lượt thi không được trả thành hàng zero. API hiện trả chi tiết đã tổng hợp cho toàn kỳ; phân trang phía trình duyệt chỉ phù hợp quy mô vừa. Với hàng triệu học viên cần thêm paging DB và chỉ mục bao phủ theo execution plan của MySQL trước khi đặt SLA dưới 2 giây.

## Chạy và kiểm thử

- Backend: `cd "Adaptive Learning"; .\gradlew.bat test`
- Frontend: `cd AdaptiveLearningFE; npm run build`
- Seed demo: chạy `src/main/resources/db/report-demo-seed.sql` trên database bỏ đi sau khi đã có ít nhất một user và exam. Script thêm 3.000 lượt trong 90 ngày; không tự chạy khi ứng dụng khởi động.
- `.env.example` liệt kê biến môi trường, không chứa thông tin xác thực thật.
