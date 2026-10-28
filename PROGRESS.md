# Báo cáo tiến độ phát triển Adaptive Learning AI

> Đánh giá tĩnh từ mã nguồn trong repo tại ngày 2026-10-28 (Asia/Ho_Chi_Minh). Đây là ước lượng kỹ thuật, không phải số liệu telemetry hay cam kết nghiệm thu.

## I. Tổng quan dự án

- **Tên:** Adaptive Learning AI.
- **Mục tiêu thể hiện trong README:** nền tảng luyện thi theo mục tiêu, gồm thi thử, phân tích tiến độ/khoảng trống kiến thức, lập kế hoạch học và hỗ trợ AI.
- **Công nghệ:** Java 17, Spring Boot 3.2.3, Spring Security/JWT, Spring Data JPA, MyBatis, MySQL; Vue 3, Vite, Pinia, Vue Router; dịch vụ Python `pyBKT` dùng `http.server`, `pandas` và `pyBKT`.
- **Cấu hình dữ liệu:** MySQL Connector/J; profile mặc định `local`, DB URL cấu hình qua `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; `ddl-auto: update`. Không tìm thấy Flyway/Liquibase; có SQL nâng cấp thủ công và seed demo.
- **Module backend:** adaptive, ai, analytics, attempts, billing, certificates, classes, contentgen, exams, notifications, planner, practice, questions, rag, reports, speaking, topics, tutor, users; thêm shared/config.
- **Frontend:** Auth, User (home và các view dashboard/plans/courses/tests/history/progress/tutor/settings, thanh toán), Admin (home, reports, exam builder, knowledge-base qua các màn hình admin), trang lỗi. Có route Teacher nhưng chưa thấy màn hình/tích hợp nghiệp vụ teacher tương ứng.
- **Thời điểm:** 2026-10-28.
- **Tình trạng:** nhiều luồng cốt lõi đã có backend và một phần tích hợp Vue; các luồng thi, AI, nội dung, admin và billing có mức độ triển khai không đồng đều. Chưa có căn cứ nghiệm thu toàn hệ thống.
- **Giới hạn:** không chạy backend/frontend/test, không kết nối DB, mail, Google OAuth, Stripe/PayPal hoặc BKT; không xác nhận cấu hình triển khai thật. Các thay đổi đang có trong worktree được giữ nguyên và được xem như trạng thái code tại thời điểm đọc; không xác minh lịch sử/tính chủ đích của chúng. Tên class, route và test tồn tại không được coi là bằng chứng runtime.

### Cách ước lượng

Phần trăm là đánh giá tương đối, dựa trên độ phủ nghiệp vụ quan sát được ở giao diện/API, luồng lưu trữ, tích hợp và test source. Khi một phần chạy thử chưa được xác nhận, điểm bị giảm; không có trọng số tự động và không phải thước đo tuyệt đối. `Cần kiểm thử` nghĩa là có triển khai đáng kể nhưng hoạt động chưa được xác minh; `Đang phát triển` nghĩa là còn thiếu một hay nhiều phần nghiệp vụ/integration quan trọng. Chưa thấy module độc lập riêng cho `classes`; không tính nó như tính năng đã có.

## II. Bảng tổng quan tiến độ

Mỗi dòng dưới đây là một năng lực/module được nhóm để tránh đếm trùng các endpoint trong cùng luồng. Trạng thái trong các cột là loại trừ lẫn nhau.

| STT | Module | Tổng chức năng | Hoàn thành | Đang phát triển | Chưa triển khai | Cần kiểm thử | Tiến độ ước tính |
|---:|---|---:|---:|---:|---:|---:|---:|
| 1 | Tài khoản và xác thực | 2 | 0 | 0 | 0 | 2 | 75% |
| 2 | Danh mục chứng chỉ/chủ đề | 2 | 0 | 1 | 0 | 1 | 55% |
| 3 | Ngân hàng câu hỏi và duyệt nội dung | 1 | 0 | 1 | 0 | 0 | 50% |
| 4 | Đề thi và template | 1 | 0 | 1 | 0 | 0 | 60% |
| 5 | Thi thử/attempt | 1 | 0 | 0 | 0 | 1 | 75% |
| 6 | Analytics, gap và khuyến nghị | 1 | 0 | 1 | 0 | 0 | 55% |
| 7 | Adaptive/BKT | 1 | 0 | 1 | 0 | 0 | 55% |
| 8 | Kế hoạch học | 1 | 0 | 0 | 0 | 1 | 70% |
| 9 | Gói luyện tập | 1 | 0 | 1 | 0 | 0 | 35% |
| 10 | AI grading/rubric | 1 | 0 | 1 | 0 | 0 | 55% |
| 11 | Sinh nội dung AI | 1 | 0 | 1 | 0 | 0 | 45% |
| 12 | Tutor AI | 1 | 0 | 1 | 0 | 0 | 50% |
| 13 | Tài liệu/RAG | 1 | 0 | 1 | 0 | 0 | 50% |
| 14 | Speaking | 1 | 0 | 1 | 0 | 0 | 35% |
| 15 | Thông báo | 1 | 0 | 0 | 0 | 1 | 65% |
| 16 | Billing/thanh toán | 1 | 0 | 0 | 0 | 1 | 70% |
| 17 | Admin/user operations | 1 | 0 | 1 | 0 | 0 | 55% |
| 18 | Admin reports | 1 | 0 | 0 | 0 | 1 | 70% |
| 19 | Classes/teacher workflow | 1 | 0 | 0 | 1 | 0 | 0% |
|  | **Tổng (chức năng nhóm)** | **21** | **0** | **12** | **1** | **8** | **~55%** |

> Điểm tổng ~55% là trung bình tham khảo của các dòng, không phải KPI tiến độ chính thức. Module tổng hợp admin được tính riêng với quản lý người dùng/nội dung để không gộp nhầm các flow khác nhau.

## III. Chi tiết theo module

| STT | Chức năng | Frontend | Backend | Database | Tích hợp | Kiểm thử | Trạng thái | Tiến độ |
|---:|---|---|---|---|---|---|---|---:|
| 1 | Đăng ký, xác minh email, đăng nhập, Google OAuth/profile | Có trang login/register/verify và service | Có auth controller, JWT, xác minh email, Google verifier | User và email verification qua JPA | Vue gọi auth API | Có unit/integration tests; chưa chạy | Cần kiểm thử | 75% |
| 2 | Phân quyền và quản trị tài khoản | Admin home gọi API user | Role/security, admin CRUD/status/role/plan | User/Role JPA | Admin user flows tích hợp một phần | Có security/user tests; chưa chạy | Cần kiểm thử | 75% |
| 3 | Chứng chỉ và topic tree | Hiển thị/chọn certificate; admin có thao tác certificate | CRUD certificate và topic endpoints | Entity/repository và initializer | Tích hợp qua learning/admin service | Chưa thấy test module riêng | Đang phát triển | 55% |
| 4 | Quản lý câu hỏi, lọc, review/approve/reject | Admin có ExamBuilder và API service; không thấy màn hình quản lý toàn diện độc lập | CRUD, lọc, random, workflow review | Question/repository | Một phần luồng admin gọi API; chưa xác nhận mọi thao tác | Chưa thấy test module riêng | Đang phát triển | 50% |
| 5 | Exam template và sinh đề | ExamBuilderDialog gọi API tạo template/sinh đề | CRUD template và generate exam | Exam/template/question entities | Có kết nối admin/user đến generate | Chưa thấy test module exams | Đang phát triển | 60% |
| 6 | Làm bài, lưu đáp án, nộp, chấm câu khách quan, lịch sử | Tests/session/result/history views nằm trong Home; learningService có các request | Start, get, patch answers, submit, result, history; khóa attempt theo owner/trạng thái/thời gian | ExamAttempt/answer/event và exam/question | Request Vue/BE khớp theo endpoint đã đọc | Có AttemptService tests; chưa chạy, chưa E2E | Cần kiểm thử | 75% |
| 7 | Dashboard tiến độ, gap analysis, recommendation | Home/ProgressView gọi progress, history và recommendations | Aggregation/progress, analyze attempt, start/complete recommendation | LearningProgress, GapAnalysis, Recommendation | Có các API được learningService gọi | Có tests analytics; chưa chạy; chưa thấy E2E | Đang phát triển | 55% |
| 8 | Adaptive recommendation và dự báo/BKT | API client gọi recommend; không thấy UI riêng đầy đủ cho profile/forecast | Heuristic và BktClient; pyBKT endpoint `/v1/mastery/estimate` | Trạng thái learner dựa các entity adaptive | Tích hợp HTTP tùy dịch vụ tại `adaptive.bkt.base-url`; fallback heuristic khi lỗi | Có unit/simulation tests; chưa chạy | Đang phát triển | 55% |
| 9 | Lập kế hoạch học/task | Home có tạo/cập nhật/hoàn tất/lưu trữ plan | API và ownership/lifecycle logic | StudyPlan/PlanTask | Vue `learningService` khớp các endpoint chính | Có planner lifecycle/flow/ownership tests; chưa chạy | Cần kiểm thử | 70% |
| 10 | Gói luyện tập | Có API gọi lấy danh sách nhưng UI/flow bắt đầu gói chưa được xác nhận | CRUD package; API practice | PracticePackage | Liên kết user-service mỏng; không thấy client đủ cho CRUD admin | Chưa thấy test module riêng | Đang phát triển | 35% |
| 11 | AI grading và rubric | Không thấy giao diện nộp bài/chấm/xem job được nối rõ trong các view đã liệt kê | Grade job, evaluation, retry/admin jobs, rubric API | AiJob/AiEvaluation/Rubric | Chưa thấy service Vue gọi `/api/v1/ai` | Có service/simulation tests; chưa chạy | Đang phát triển | 55% |
| 12 | Sinh câu hỏi bằng AI và áp dụng kết quả | Admin service có generate/apply; không xác nhận UI đầy đủ để theo dõi job/error | Content-generation submit/list/get/apply; admin-only | ContentGeneration | Một phần client; chưa xác nhận UX/recovery | Có 1 test module; chưa chạy | Đang phát triển | 45% |
| 13 | AI tutor/conversations | TutorView; Home xử lý hội thoại/tin nhắn | CRUD conversations/messages, gọi AI service | Conversation/message persistence trong module | `learningService` gọi endpoints chính | Có test tutor; chưa chạy; upstream AI chưa xác minh | Đang phát triển | 50% |
| 14 | Tài liệu học và RAG | Admin Home/OverlayDemo quản lý knowledge base; không thấy tích hợp learner RAG rõ trong UI | Upload text/PDF, quản lý tài liệu, chunks/search/context | StudyDocument/DocumentChunk | Admin API có client; learner RAG chưa thấy client tương ứng | Có PDF extractor tests; chưa chạy, chưa E2E | Đang phát triển | 50% |
| 15 | Nộp và đánh giá Speaking | Không thấy UI ghi âm/nộp bài/tương tác evaluation | Submission/evaluation API, entities/repository | SpeakingSubmission/Evaluation | Chưa thấy frontend service gọi `/speaking` | Có service test; chưa chạy | Đang phát triển | 35% |
| 16 | Notification inbox/SSE | Home dùng inbox, mark read và listen | Inbox, read, SSE stream | UserNotification | Service kết nối các API | Có notification test; chưa chạy, chưa xác minh SSE runtime | Cần kiểm thử | 65% |
| 17 | Gói premium và thanh toán | Pricing/status/sandbox; paymentService gọi checkout/status/history/sandbox | Stripe/PayPal/sandbox strategies, webhook, entitlement/quota, admin billing | Payment, webhook event, subscription plan; JPA update | Luồng sandbox/frontend nối API; provider thật cần credential/webhook config | Có unit/simulation/provider tests; chưa chạy, chưa có xác nhận provider thật | Cần kiểm thử | 70% |
| 18 | Báo cáo quản trị | Trang `/admin/reports` gọi dữ liệu và export | `GET /api/v1/reports`, XLSX/PDF; ROLE_ADMIN; aggregate DB theo ngày/đề/user | Query aggregate từ dữ liệu attempt/exam/user | API/UI có liên kết và cùng filter cơ bản | Có ReportService tests; chưa chạy; export chưa xác minh đầu cuối | Cần kiểm thử | 70% |
| 19 | Lớp học và workflow teacher | Có route Teacher nhưng chưa thấy page/API flow tương ứng | Thư mục `classes` không có file triển khai; không có controller | Không tìm thấy entity/repository classes trong module | Chưa có bằng chứng tích hợp | Không thấy test | Chưa triển khai | 0% |

### Chức năng và API liên quan

Các endpoint bên dưới lấy từ mapping trong controller; trừ báo cáo có prefix tự khai báo, các API dùng prefix `/api/v1`. Việc liệt kê xác nhận route có trong source, không xác nhận server đang chạy hay API đã được UI sử dụng. Các endpoint có ghi “chưa thấy frontend gọi” là khoảng trống tích hợp dựa trên service/Vue caller đã khảo sát.

| Chức năng | API backend liên quan | Frontend/tình trạng tích hợp |
|---|---|---|
| Đăng ký, xác minh email, đăng nhập, Google và profile | `POST /auth/login`, `POST /auth/google`, `GET /auth/google/config`, `POST /auth/signup`, `POST /auth/signup/verify`, `POST /auth/signup/resend`, `GET /auth/profile` | `userService.js`, Login/Register/VerifyEmail gọi các API auth chính. |
| Quản trị người dùng, role, trạng thái, plan, thống kê | `GET /admin/users`, `GET /admin/users/analytics`, `GET /admin/users/{userId}/attempts`, `GET /admin/users/{userId}/progress?certificateId=...`, `POST /admin/users`, `PUT /admin/users/{userId}/role`, `PATCH /admin/users/{userId}/status`, `PUT /admin/users/{userId}/plan`, `GET /admin/users/payments` | `adminService.js` và Admin Home gọi các thao tác chính. Những đường dẫn này thuộc AdminController; route prefix API đầy đủ là `/api/v1`. |
| Chứng chỉ | `GET /certificates`, `GET /certificates/{id}`, `POST /certificates`, `PUT /certificates/{id}`, `DELETE /certificates/{id}` | Đọc/chọn qua learning/admin service; admin có tạo/sửa/xóa trong Admin Home. Tạo/sửa/xóa yêu cầu admin. |
| Chủ đề/topic tree | `GET /topics`, `GET /topics/certificate/{certificateId}`, `GET /topics/certificate/{certificateId}/tree`, `GET /topics/type/{type}`, `GET /topics/{id}`, `GET /topics/{id}/children`, `POST /topics`, `PUT /topics/{id}`, `DELETE /topics/{id}` | `adminService.topics()` gọi danh sách theo certificate; chưa thấy frontend khai thác toàn bộ tree/type/children. Ghi/sửa/xóa dành cho admin. |
| Ngân hàng câu hỏi và quy trình duyệt | `GET /questions/random`, `GET /questions`, `GET /questions/{id}`, `POST /questions`, `PUT /questions/{id}`, `POST /questions/{id}/submit-review`, `POST /questions/{id}/approve`, `POST /questions/{id}/reject` | `adminService.js` gọi lọc/tạo/sửa/duyệt; ExamBuilderDialog dùng truy vấn candidate. Chưa thấy UI quản lý vòng review đầy đủ. |
| Exam và template | `GET /exams`, `GET /exams/{id}`, `GET /exams/{id}/detail`, `DELETE /exams/{id}`, `POST /exams/{id}/archive`; `GET /exam-templates`, `POST /exam-templates`, `DELETE /exam-templates/{id}`, `POST /exam-templates/{id}/generate` | `learningService.js` gọi xem đề/template và generate; `adminService.js` + ExamBuilderDialog gọi tạo template/generate. Chưa có test module exams được tìm thấy. |
| Thi thử, lưu đáp án, nộp và lịch sử | `POST /attempts/start`, `GET /attempts/{id}`, `PATCH /attempts/{id}/answers`, `POST /attempts/{id}/submit`, `GET /attempts/{id}/result`, `POST /attempts/{id}/violations`, `GET /attempts/me/history` | `learningService.js` gọi start/get/save/submit/history; Home/views cung cấp luồng tests/session/result/history. Test source có, chưa chạy. |
| Tiến độ, gap analysis, khuyến nghị | `POST /analytics/attempts/{attemptId}/analyze`, `GET /analytics/attempts/{attemptId}/analysis`, `GET /analytics/me/progress?certificateId=...`, `GET /analytics/me/recommendations`, `POST /analytics/recommendations/{id}/start`, `POST /analytics/recommendations/{id}/complete` | `learningService.js` gọi progress/recommendations/start; chưa thấy Vue gọi analyze/analysis/complete trực tiếp trong lượt rà soát. |
| Adaptive learning/BKT | `GET /adaptive/profile`, `POST /adaptive/attempts/{attemptId}/update`, `GET /adaptive/recommend?certificateId=...`, `GET /adaptive/forecast?certificateId=...`; dịch vụ Python `POST /v1/mastery/estimate` trên host/port cấu hình BKT | Service Vue chỉ thấy gọi `GET /adaptive/recommend`; chưa thấy tích hợp UI riêng cho profile/update/forecast. Backend có fallback heuristic nếu BKT không sẵn sàng. |
| Kế hoạch học và task | `POST /plans`, `GET /plans`, `GET /plans/{id}`, `GET /plans/{id}/tasks`, `GET /plans/{id}/summary`, `PATCH /plans/tasks/{taskId}`, `POST /plans/{id}/complete`, `POST /plans/{id}/archive` | `learningService.js` gọi các API chính; Home/Plans view tích hợp tạo/cập nhật/hoàn tất/lưu trữ. Có tests planner nhưng chưa chạy. |
| Gói luyện tập | `GET /practice?certificateId=...`, `GET /practice/{id}`, `POST /practice`, `PUT /practice/{id}`, `DELETE /practice/{id}` | Frontend có gọi danh sách package; chưa thấy client/UI tích hợp đọc chi tiết hay CRUD admin. Ghi/sửa/xóa dành cho admin. |
| AI grading, rubric và job | `POST /ai/grade`, `GET /ai/evaluations/{id}`, `GET /ai/evaluations/attempt/{attemptId}`, `POST /ai/jobs/{id}/retry`, `GET /ai/jobs`, `POST /ai/rubrics`, `PUT /ai/rubrics/{id}`, `GET /ai/rubrics/certificate/{certificateId}` | Chưa tìm thấy Vue service gọi `/api/v1/ai`; backend có role/entitlement checks theo controller. Job/rubric UI chưa thấy tích hợp. |
| Sinh nội dung/câu hỏi bằng AI | `POST /content-generation`, `GET /content-generation`, `GET /content-generation/{id}`, `POST /content-generation/{id}/apply` | `adminService.js` có submit/apply; chưa xác nhận giao diện trạng thái chi tiết/list/retry và xử lý lỗi đầy đủ. Controller giới hạn admin. |
| Tutor AI | `POST /tutor/conversations`, `GET /tutor/conversations`, `GET /tutor/conversations/{id}/messages`, `POST /tutor/conversations/{id}/messages`, `DELETE /tutor/conversations/{id}` | `learningService.js` gọi tạo/list/messages/ask; Home và TutorView tích hợp hội thoại. Chưa xác minh AI provider/runtime. |
| Knowledge base admin và RAG người học | Admin: `POST /admin/knowledge-base`, `POST /admin/knowledge-base/pdf`, `GET /admin/knowledge-base`, `GET /admin/knowledge-base/{id}`, `PUT /admin/knowledge-base/{id}`, `PATCH /admin/knowledge-base/{id}/active`, `DELETE /admin/knowledge-base/{id}`. RAG: `POST /rag/documents`, `GET /rag/documents`, `GET /rag/documents/{id}`, `GET /rag/documents/{id}/search?query=...`, `DELETE /rag/documents/{id}` | Admin Home/OverlayDemo gọi API quản lý tài liệu. Chưa thấy frontend gọi API learner `/rag`; PDF upload/service có mặt ở adminService. |
| Speaking submission/evaluation | `POST /speaking/submissions`, `GET /speaking/submissions/{id}`, `GET /speaking/submissions/attempt/{attemptId}`, `POST /speaking/evaluations`, `GET /speaking/evaluations/{id}`, `GET /speaking/evaluations/submission/{submissionId}` | Chưa tìm thấy frontend service/view gọi `/api/v1/speaking`; cần tích hợp luồng ghi âm/nộp/xem đánh giá và xác minh dịch vụ chấm. |
| Thông báo | `GET /notifications`, `PATCH /notifications/{id}/read`, `GET /notifications/stream` (SSE) | `notificationService.js` + User Home gọi inbox/mark-read/listen. Chưa xác minh SSE runtime/reconnect. |
| Billing/entitlement/thanh toán | `GET /me/entitlements`; `POST /payments/checkout`; `GET /payments/{id}`; `GET /payments/history`; `GET /payments/paypal/return`; `POST /payments/sandbox/complete`; webhook `POST /api/webhooks/stripe`, `POST /api/webhooks/paypal`. Admin: `GET/POST /admin/plans`, `PUT /admin/plans/{id}`, `PATCH /admin/plans/{id}/toggle`, `DELETE /admin/plans/{id}`, `GET /admin/billing/users`, `GET /admin/billing/users/{userId}/payments`, `POST /admin/billing/users/{userId}/reset-quota` | `paymentService.js` gọi entitlement/checkout/status/history/sandbox; Pricing/Status/Sandbox pages tích hợp sandbox path. Admin Home gọi billing/plans. Webhook và provider thật chưa xác minh. |
| Admin reports | `GET /api/v1/reports?from=YYYY-MM-DD&to=YYYY-MM-DD`, `GET /api/v1/reports/export.xlsx?from=...&to=...`, `GET /api/v1/reports/export.pdf?from=...&to=...` | Trang `/admin/reports` gọi report và export; endpoint controller yêu cầu `ROLE_ADMIN`. Chưa xác minh file export trên runtime. |
| Classes/teacher | Không tìm thấy endpoint controller cho lớp học/teacher trong module `classes` | Có route Teacher nhưng chưa thấy page nghiệp vụ hoặc API client tương ứng. Đánh giá: chưa triển khai trong source đã khảo sát. |

### Bằng chứng theo nhóm

- **Auth/users:** `Adaptive Learning/src/main/java/adaptivelearning/module/users/controller/AuthController.java`, `AdminController.java`, `UserController.java`; `Adaptive Learning/src/main/java/adaptivelearning/config/app/SecurityConfig.java`; frontend `AdaptiveLearningFE/src/features/Auth/pages/`, `src/services/userService.js`, `src/stores/user.js`. Test gồm email registration/verification, bootstrap security, authorization và JWT; kết quả chạy chưa xác minh.
- **Certificate/topic/question/exams:** controllers tương ứng tại `module/certificates`, `topics`, `questions`, `exams`; entity/repository ở cùng module; `AdaptiveLearningFE/src/services/adminService.js`, `learningService.js`, `src/features/Admin/components/ExamBuilderDialog.vue`. Chưa thấy Flyway/Liquibase; profile dùng JPA schema update và initializer, nên trạng thái schema mới/cũ cần kiểm tra trên DB thật.
- **Attempts:** `module/attempts/controller/AttemptController.java`, `service/impl/AttemptServiceImpl.java`, `entity/ExamAttempt.java`; frontend `learningService.js`, `features/User/components/views/ExamSessionView.vue`, `ExamResultView.vue`; test `AttemptServiceImplTest.java`. Code có kiểm tra owner/trạng thái/expire và transaction; behavior race, timeout scheduler, client reconnect và serialization vẫn cần tích hợp kiểm thử.
- **Analytics/adaptive/planner:** controller/service/repository tương ứng; `Adaptive Learning/src/main/java/adaptivelearning/module/adaptive/service/BktClient.java`, `Adaptive Learning/pybkt/app.py`; Vue `learningService.js`, các view progress/plans. Test planner/adaptive/analytics/BKT có trong repo nhưng chưa chạy. BKT cần đủ training history; service trả `ready:false` khi chưa đủ người học/dữ liệu và adaptive có fallback heuristic.
- **AI/contentgen/tutor/RAG/speaking:** controller/service/entities tại các package tương ứng; `Adaptive Learning/src/main/java/adaptivelearning/module/ai/gateway/OpenAiProvider.java`; frontend có TutorView và admin knowledge-base/content-gen service call, nhưng chưa tìm được Vue caller cho `/api/v1/ai`, `/api/v1/speaking` hoặc learner `/api/v1/rag`. Đây là backend code có nhưng frontend integration chưa thấy bằng chứng, không đồng nghĩa API lỗi.
- **Notifications:** `module/notifications/controller/NotificationController.java`, `NotificationService.java`, `AdaptiveLearningFE/src/services/notificationService.js`; Home gọi inbox/stream/mark-read. SSE cần xác minh reconnect, auth và proxy timeout khi chạy.
- **Billing:** `module/billing/controller/`, `PaymentService.java`, provider strategies, `AdaptiveLearningFE/src/services/paymentService.js`, Pricing/PaymentStatus/PaymentSandbox. `SandboxPaymentStrategy` là đường thử nghiệm, không chứng minh thanh toán thật. Stripe/PayPal phụ thuộc secret/callback/webhook đúng môi trường; test nguồn có nhưng chưa chạy.
- **Reports:** `module/reports/controller/ReportController.java`, `ReportService.java`, `ReportRepository.java`, `AdaptiveLearningFE/src/features/Admin/pages/Reports.vue`, `docs/reports.md`. Mốc ngày báo cáo dùng Asia/Ho_Chi_Minh và khoảng nửa mở theo tài liệu/mã; exporter và query DB thật chưa kiểm chứng.
- **Classes/teacher:** chỉ thấy package backend trống và route Teacher; chưa thấy endpoint hay màn hình nghiệp vụ tương ứng. Chỉ kết luận chưa tìm thấy trong repo được khảo sát.

## IV. Các chức năng chưa hoàn thiện (ưu tiên)

| Ưu tiên | Chức năng | Đã có | Còn thiếu/rủi ro | Việc tiếp theo |
|---|---|---|---|---|
| P1 | Thi thử đầu cuối | API start/save/submit/result, entity và Vue views | Chưa chạy test; chưa xác minh migration/schema, hết giờ, reconnect và FE/BE trên DB | Chạy suite attempt/security; E2E start-save-expire-submit-result với MySQL sạch và dữ liệu nâng cấp |
| P1 | AI grading trong trải nghiệm học viên | Job/evaluation/rubric backend | Không thấy frontend client/UI; chưa xác minh cấu hình provider, schema output, retry và trạng thái job | Nối UI/service; kiểm thử provider giả, timeout, response sai schema, retry và quyền sở hữu |
| P1 | Speaking | API submission/evaluation | Không thấy giao diện ghi âm/nộp bài và kết nối endpoint; speech provider chưa xác minh | Chốt workflow/audio storage/provider; nối UI và kiểm thử giới hạn file/ownership/lỗi provider |
| P1 | Quản trị câu hỏi và nội dung | API CRUD/review, builder dùng question API | Không thấy UI duyệt câu hỏi đầy đủ; nhiều test module không có | Hoàn thiện màn hình/editor/review; bổ sung validation, quyền và integration tests |
| P1 | Classes/teacher | Chỉ route/package | Không thấy nghiệp vụ thực tế | Xác định phạm vi sản phẩm; sau đó thiết kế API/schema/role/UI/test hoặc gỡ route nếu ngoài phạm vi |
| P2 | Analytics/adaptive | API gap/progress/recommend/BKT | Chưa xác minh chất lượng công thức, dữ liệu ít, drift giữa topic/skill và biểu diễn UI | Kiểm thử dữ liệu biên, giải thích kết quả, fallback và tích hợp pyBKT container |
| P2 | Tutor/RAG/content generation | Backend và một phần UI/admin service | Chưa xác minh provider; UI theo dõi job, lỗi, dữ liệu nguồn và retry chưa rõ; frontend learner RAG chưa thấy | Chạy với provider giả; bổ sung trạng thái/ownership/giới hạn upload và nối đúng workflow |
| P2 | Billing | Provider, webhook, entitlements, sandbox UI | Provider thật và webhook chưa xác minh; consistency/duplicate/concurrent event cần DB integration | Chạy simulation tests; kiểm tra webhook signature, idempotency/locking, rollback/refund và sandbox vô hiệu hóa ngoài dev |
| P2 | Notifications/report exports | API/UI và tests nguồn | SSE/export chưa chạy; quyền, timezone và kết quả PDF/XLSX chưa xác minh | Kiểm thử browser/API với DB; xác nhận file, khoảng ngày, role và ngắt kết nối |
| P2 | Database lifecycle | MySQL + `ddl-auto:update` + scripts thủ công | Không có migration tool; schema update production và dữ liệu cũ chưa có bằng chứng tái lập | Thiết lập migration có version, test từ schema rỗng và schema nâng cấp; rà soát timezone lưu trữ |

## V. Vấn đề kỹ thuật và điểm cần xác minh

1. **Schema deployment:** `application-local.yml` và `application-dev.yml` đặt `spring.jpa.hibernate.ddl-auto: update`; có SQL upgrade thủ công, không thấy Flyway/Liquibase. Đây là rủi ro lặp lại deploy/rollback, chưa kết luận DB hiện tại thiếu bảng.
2. **Coverage frontend/backend chưa đồng đều:** không thấy Vue caller cho AI grading, speaking, learner RAG và API quản lý Practice CRUD; API tồn tại không có nghĩa đã tích hợp UI.
3. **Quản lý lớp/teacher:** package `classes` không có file triển khai và route Teacher không có page nghiệp vụ tìm thấy. Cần xác nhận có nằm trong scope hay không.
4. **Kiểm thử chưa xác nhận:** có 35 file `*Test.java` theo lượt đếm tại repo; chưa chạy Gradle test nên không khẳng định test pass. Nhiều module không có test riêng: certificates, exams, practice, questions, topics, classes. Không có bằng chứng E2E frontend trong danh sách file khảo sát.
5. **External dependencies chưa xác minh:** Google OAuth, mail, AI provider, Stripe/PayPal, MySQL và dịch vụ pyBKT phụ thuộc biến môi trường, credential và dịch vụ chạy thật. Không đưa giá trị secret vào báo cáo.
6. **pyBKT readiness:** `BktClient` bắt exception và trả `Optional.empty()` để heuristic còn hoạt động; Python service yêu cầu tối thiểu hai learner trong training data và skill trùng. Cần monitor để phân biệt fallback với kết quả model.
7. **Frontend route/auth:** route metadata có `allowedRoles`; cần kiểm tra guard thực tế và security integration cho từng endpoint. Backend có `SecurityConfig` và `@PreAuthorize`, nhưng không thực hiện penetration test.
8. **Báo cáo:** export XLSX/PDF và aggregate query có code, test đơn vị hiện mới kiểm tra công thức tỷ lệ; hiệu năng, dữ liệu thật, timezone DB và tải file chưa được xác minh.
9. **Dấu hiệu mock:** `PaymentSandbox.vue` có thông tin thẻ thử nghiệm và QR placeholder; đây là giao diện sandbox, không được coi là tích hợp payment gateway production. Các TODO ở hooks frontend cần đánh giá riêng, chưa thấy thuộc luồng nghiệp vụ cốt lõi.

## VI. Kế hoạch hoàn thiện

| Giai đoạn | Công việc/module | Ưu tiên | Phụ thuộc | Tiêu chí xác nhận |
|---|---|---|---|---|
| 1. Xác lập baseline | Chạy backend tests và frontend build; khởi tạo MySQL test; ghi lại lỗi thực tế | P0 | Java/Node deps, DB test config | Có exit status/log; test không dùng DB/prod data; lỗi được phân loại và tái hiện |
| 2. Hoàn thiện luồng cốt lõi | Attempts, exams/questions, auth/authorization | P1 | Baseline; schema | E2E đăng nhập -> chọn đề -> lưu -> nộp -> xem kết quả; kiểm tra owner/role/expire và regression |
| 3. Đồng bộ AI và nội dung | AI grading, speaking, tutor, RAG/content generation | P1 | Quyết định UX/provider, secret test | FE gọi đúng API; loading/error/retry; validation/quyền; test provider timeout và response lỗi |
| 4. Chốt phạm vi quản trị | Question review, classes/teacher, practice CRUD | P1 | Product scope và role model | Mỗi chức năng trong scope có UI/API/DB/authorization/test; ngoài scope được ghi rõ |
| 5. Chuẩn hóa database | Thay schema update phụ thuộc bằng migration có version; xác nhận timezone | P2 | DB schema hiện tại và dữ liệu production | Cài mới + nâng cấp từ snapshot test thành công; rollback/backup procedure được ghi |
| 6. Xác minh billing/report/notification | Stripe/PayPal webhook, idempotency, refund; SSE; PDF/XLSX/report dates | P2 | Sandbox provider, DB, browser | Contract/integration tests; xác nhận role, signature, duplicate events, file và timezone |
| 7. Hardening và vận hành | Quan sát fallback BKT/AI, giới hạn upload/rate, logging không lộ dữ liệu, hiệu năng query | P2-P3 | Luồng đã ổn định và dữ liệu đại diện | Security review; load/profile trên môi trường tách biệt; ngưỡng và dashboard được xác định |

## VII. Kết luận

- **Tương đối đầy đủ về mặt source/integration:** Auth, attempt/thi thử, planner, notifications, billing sandbox/provider plumbing và admin reporting có đường đi backend rõ; các phần này vẫn ở trạng thái cần kiểm thử vì chưa chạy suite hay E2E trong lần đánh giá.
- **Còn dang dở rõ nhất:** AI grading, speaking, learner RAG và quản lý câu hỏi chưa có bằng chứng frontend đầy đủ; analytics/adaptive, content generation và tutor cần xác minh chất lượng nghiệp vụ/provider; teacher/classes chưa thấy triển khai thực tế.
- **Rủi ro vận hành lớn:** chưa có migration framework được tìm thấy, profile dùng schema auto-update; nhiều tích hợp ngoài phụ thuộc cấu hình; chưa có bằng chứng build/test/runtime hiện tại.
- **Thứ tự ưu tiên:** thiết lập baseline có thể tái lập; xác minh luồng thi và quyền; nối các API AI/speaking còn thiếu UI; quyết định phạm vi teacher/classes; chuẩn hóa schema; cuối cùng kiểm chứng provider, export, notification và hardening.

**Phân biệt bằng chứng:** “Đã viết code” chỉ xác nhận source tồn tại; “đã tích hợp” chỉ ghi nhận khi tìm thấy frontend gọi API/backend gọi dịch vụ tương ứng; “đã xác minh hoạt động” chỉ ghi nhận khi có kết quả chạy. Báo cáo này chưa xác minh hoạt động runtime của ứng dụng hoặc test.
