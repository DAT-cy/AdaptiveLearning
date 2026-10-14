# AI Exam Generator — Product & Implementation Specification

> Mục đích: Tài liệu đầu vào cho Codex để triển khai hoặc mở rộng ứng dụng AdaptiveLearning theo hướng import tài liệu, AI phân loại dữ liệu, quản lý ngân hàng câu hỏi, cấu hình bộ đề và sinh đề cho THPT, IELTS, TOEIC.
>
> Lưu ý: Đây là đặc tả đề xuất dựa trên yêu cầu người dùng. Khi làm việc trong repository, Codex phải ưu tiên cấu trúc, công nghệ, quy ước và tài liệu hiện có; không tự ý thay thế kiến trúc dự án.

## Phạm vi sản phẩm đã chốt trong repository này

Đặc tả gốc bên dưới có đề cập vai trò Teacher, lớp học và quy trình import tài liệu. Quyết định sản phẩm hiện tại ưu tiên mô hình học cá nhân, vì vậy áp dụng các điều chỉnh sau:

- Chỉ có hai role: `ROLE_USER` (người học) và `ROLE_ADMIN` (quản trị/biên soạn). Không tạo role hoặc module Teacher, không có lớp/nhóm học.
- USER học theo mục tiêu IELTS, TOEIC hoặc THPT; AI cá nhân hóa lộ trình, bài luyện, giải thích và trợ lý theo dữ liệu của chính USER.
- ADMIN quản lý tài khoản, danh mục kỳ thi/kỹ năng/môn, câu hỏi, duyệt nội dung và phát hành đề.
- AI sinh câu hỏi mới chỉ tạo bản nháp. Nội dung phải được ADMIN xem lại và duyệt trước khi dùng trong đề.
- MVP hiện có kho knowledge base chung do ADMIN nhập `.txt`/`.md`, gắn theo kỳ thi và topic; AI lấy các chunk khớp từ khóa, trả source chunk IDs và không sinh câu nếu không có nguồn phù hợp.
- Import XLSX/CSV, trích xuất DOCX/PDF, OCR ảnh/scan, semantic embeddings và combo đề là định hướng phase sau; không mô tả các phần này là đã triển khai.
- Mọi ví dụ/lưu ý ghi “giáo viên” trong đặc tả gốc được hiểu là ADMIN hoặc chuyên gia đánh giá độc lập, không phải một role trong sản phẩm.

Luồng nghiệp vụ MVP được triển khai được ghi tại `BUSINESS.md` mục 2.2; trạng thái và phần còn thiếu được theo dõi ở `PHASES.md`.

## 1. Mục tiêu sản phẩm

Xây dựng hệ thống cho phép:
- Giáo viên import đề thi, ngân hàng câu hỏi và tài liệu học tập từ nhiều định dạng.
- Hệ thống trích xuất nội dung, dùng AI đề xuất phân loại theo kỳ thi, môn/lớp, kỹ năng, band/dải điểm, dạng câu hỏi, độ khó và chủ đề.
- Giáo viên kiểm tra, chỉnh sửa và duyệt kết quả phân tích trước khi đưa vào ngân hàng câu hỏi chính thức.
- Admin cấu hình các kỳ thi, môn học, lớp, kỹ năng, mức điểm, bộ đề đơn và combo đề.
- Giáo viên tạo đề dựa trên cấu hình và dữ liệu đã được duyệt.
- Học sinh làm bài trên giao diện phù hợp từng loại đề và kỹ năng.
- Mỗi câu hỏi phải truy vết được nguồn tài liệu; phân biệt câu trích xuất từ tài liệu gốc với câu do AI sinh mới.

## 2. Quy tắc triển khai cho Codex

1. Trước khi sửa code, kiểm tra repository, README, package manifests, cấu trúc thư mục, migration/schema, routing, UI components, auth/role và các quy ước đang dùng.
2. Tái sử dụng stack, component, style system và pattern hiện có. Không tự ý đổi framework hoặc dựng một dự án mới bên cạnh.
3. Nếu repository đã có chức năng tương tự, mở rộng chức năng hiện tại thay vì tạo bản trùng lặp.
4. Không chỉ tạo UI tĩnh: kết nối API, validation, loading/error/empty states và persistence nếu backend hiện có hỗ trợ.
5. Không giả lập rằng AI đã phân tích thành công khi chưa có model/API thật. Nếu chưa cấu hình AI, tạo interface/service abstraction và trạng thái “chưa cấu hình”, hoặc mock chỉ trong môi trường phát triển và phải ghi rõ.
6. Không tự bịa câu hỏi thi thật, đáp án, quy định thi chính thức hoặc dữ liệu kiểm thử như dữ liệu đã xác minh.
7. Không tự tạo các con số hiệu năng hoặc độ chính xác.
8. Chia công việc thành các bước nhỏ; sau khi khảo sát, tóm tắt kế hoạch ngắn rồi triển khai.
9. Bảo đảm phân quyền ở backend, không chỉ ẩn nút trên giao diện.
10. Viết hoặc cập nhật test cho logic quan trọng nếu repo đã có test framework.

## 3. Vai trò và quyền hạn

### Admin
- Quản lý loại kỳ thi: THPT, IELTS, TOEIC và khả năng mở rộng.
- Quản lý danh mục môn học, lớp, kỹ năng, dạng câu hỏi và mức độ.
- Tạo cấu hình đề đơn và combo đề.
- Cấu hình số lượng câu, thời lượng, tỷ lệ dạng câu hỏi, độ khó, cách tính điểm và quy tắc chọn câu.
- Quản lý người dùng/phân quyền nếu tính năng đó đã thuộc phạm vi dự án.
- Theo dõi tình trạng tài liệu import và dữ liệu ngân hàng câu hỏi.

### Teacher
- Import tài liệu.
- Xem tiến trình xử lý, lỗi trích xuất và kết quả AI đề xuất.
- Chỉnh sửa metadata, câu hỏi, lựa chọn, đáp án, lời giải và nhóm câu hỏi.
- Duyệt hoặc từ chối câu hỏi.
- Tạo đề đơn/combo theo cấu hình được phép.
- Xem trước đề, lưu nháp, phát hành đề và quản lý đề đã tạo.
- Chỉ truy cập dữ liệu thuộc phạm vi quyền được cấp.

### Student
- Xem danh sách đề được phép làm.
- Lọc/chọn đề theo kỳ thi, lớp, môn, kỹ năng hoặc mức điểm.
- Làm bài, lưu tiến độ, đánh dấu câu, chuyển câu và nộp bài.
- Xem kết quả/giải thích theo chính sách của đề.

## 4. Luồng nghiệp vụ chính

### 4.1 Import tài liệu
1. Giáo viên chọn hoặc kéo-thả file.
2. Chọn loại dữ liệu nếu biết: đề hoàn chỉnh, ngân hàng câu hỏi, hoặc tài liệu học tập; có thể chọn “AI tự nhận diện”.
3. Có thể cung cấp metadata gợi ý: kỳ thi, lớp, môn, kỹ năng, mức điểm. Metadata AI dự đoán phải phân biệt với metadata do người dùng xác nhận.
4. Backend kiểm tra định dạng, dung lượng, quyền truy cập và lưu file gốc.
5. Trích xuất text/layout; dùng OCR khi cần với PDF scan/ảnh.
6. AI phân tích và trả về dữ liệu có cấu trúc.
7. Hiển thị trang review cho giáo viên; làm rõ trường thiếu, mâu thuẫn hoặc confidence thấp.
8. Chỉ dữ liệu đã được duyệt mới được dùng làm nguồn sinh đề chính thức.

### 4.2 AI phân loại
AI có thể đề xuất:
- exam_type: THPT / IELTS / TOEIC / OTHER / UNKNOWN
- subject
- grade_level
- skill
- target_score_min, target_score_max hoặc target_band_min, target_band_max
- question_type
- difficulty
- topic
- source question number/page/section
- answer choices, correct answer, explanation nếu tài liệu có
- passage/audio/group relationship
- confidence hoặc review flags

AI không được khẳng định một phân loại thiếu căn cứ. Với dữ liệu mơ hồ, đánh dấu NEEDS_REVIEW và yêu cầu giáo viên xác nhận.

### 4.3 Sinh đề
1. Người dùng chọn cấu hình đề.
2. Backend truy vấn ngân hàng câu hỏi đã duyệt và phù hợp điều kiện.
3. Kiểm tra số lượng, cấu trúc, nhóm phụ thuộc và quy tắc kỳ thi.
4. Nếu không đủ dữ liệu, trả lỗi/ cảnh báo có giải thích và số lượng hiện có; không tự âm thầm tạo đề sai chuẩn.
5. Chọn câu hỏi theo quy tắc cấu hình; lưu snapshot các câu hỏi vào đề để đề đã phát hành không thay đổi khi ngân hàng cập nhật.
6. Nếu có câu AI sinh mới, đánh dấu nguồn AI và áp dụng quy trình duyệt tương ứng.
7. Render đề theo dạng câu hỏi/kỹ năng.

## 5. Hỗ trợ định dạng import

Định hướng hỗ trợ:
- XLSX / CSV: định dạng ưu tiên cho ngân hàng câu hỏi có cấu trúc.
- DOCX / DOC: tài liệu Word.
- PDF có text và PDF scan.
- PNG / JPG / JPEG: ảnh đề thi.
- Có thể mở rộng định dạng khác dựa trên thư viện và stack hiện có.

### Mẫu Excel/CSV gợi ý
Các cột có thể gồm:
- external_id
- exam_type
- subject
- grade_level
- skill
- target_score_min
- target_score_max
- target_band_min
- target_band_max
- section
- question_type
- question_text
- option_a
- option_b
- option_c
- option_d
- correct_answer
- explanation
- difficulty
- topic
- passage_id
- source_reference

Không yêu cầu tất cả cột có mặt ở mọi kỳ thi. Cần có template tải xuống, kiểm tra cột bắt buộc, preview dữ liệu, báo lỗi theo dòng và chống import trùng theo quy tắc rõ ràng.

## 6. Quy tắc riêng theo kỳ thi

### 6.1 THPT
- Có cấu hình theo lớp 10, 11, 12 và các lớp/danh mục có thể được Admin quản lý.
- Admin quyết định môn nào được dùng cho từng lớp và trong combo nào.
- Ví dụ cấu hình minh họa (không phải quy định chương trình chính thức):
  - Lớp 10: Toán, Ngữ văn.
  - Lớp 11: Toán, Ngữ văn, Tiếng Anh.
  - Lớp 12: Toán, Ngữ văn, Tiếng Anh và môn lựa chọn.
- Không hard-code ví dụ trên vào logic. Tất cả phải chỉnh sửa qua cấu hình.
- Hỗ trợ đề đơn môn và combo nhiều môn.
- Mỗi môn/section có số câu, thời gian, dạng câu, độ khó và cách chấm điểm riêng khi cần.

### 6.2 IELTS
- Hỗ trợ bốn kỹ năng: Listening, Reading, Writing, Speaking.
- Admin cấu hình các band mục tiêu/range tùy nhu cầu, ví dụ 4.5–5.5, 6.0–6.5, 7.0–8.0.
- Câu hỏi/tài liệu phải gắn kỹ năng và band/range khi có căn cứ.
- Không giả định mọi band có cùng cấu trúc hoặc mọi kỹ năng đều là trắc nghiệm.
- Listening cần audio và liên kết nhóm câu hỏi; Reading cần passage; Writing cần prompt/rubric; Speaking cần prompt/part và rubric nếu có.
- Nếu dữ liệu không đủ cho một kỹ năng/band, báo rõ số câu/tài liệu phù hợp đang có.

### 6.3 TOEIC
- Hỗ trợ các phần Listening và Reading; cho phép Admin cấu hình cấu trúc và dải điểm mục tiêu.
- Không hard-code duy nhất các dải điểm. Admin có thể thêm/sửa cấu hình.
- Với Listening, lưu liên kết audio, phần thi và các câu hỏi cùng audio.
- Với Reading, lưu passage hoặc ngữ cảnh chung cho các câu liên quan.
- Chỉ gắn dải điểm mục tiêu nếu dữ liệu/giáo viên cung cấp đủ căn cứ; không suy ra điểm TOEIC chính xác từ độ khó câu hỏi nếu chưa có mô hình hiệu chuẩn.

## 7. Cấu hình đề thi của Admin

Cần có các thực thể/cấu hình khái niệm:
- ExamType: loại kỳ thi.
- Subject: môn học.
- GradeLevel: lớp/cấp học.
- Skill: kỹ năng.
- ScoreBand/TargetRange: dải điểm hoặc band mục tiêu.
- ExamConfiguration: cấu hình tạo đề.
- ExamBundle: combo đề gồm nhiều môn/kỹ năng.
- ExamSection: phần thi bên trong cấu hình/combo.
- QuestionSelectionRule: quy tắc chọn câu.
- ScoringRule: quy tắc chấm điểm.

Một cấu hình đề nên định nghĩa:
- Tên, mã, kỳ thi, mô tả và trạng thái active/inactive.
- Điều kiện lớp/môn/kỹ năng/band.
- Số lượng câu và thời lượng.
- Cấu trúc phần thi, loại câu và tỷ lệ độ khó.
- Quy tắc random/chống lặp và cách xử lý khi thiếu câu.
- Cách chấm điểm và hiển thị kết quả.
- Các nguồn dữ liệu được phép dùng, nếu cần giới hạn theo giáo viên/lớp.

Tạo 3 kiểu cấu hình:
1. Đề đơn: một môn hoặc một kỹ năng.
2. Combo: nhiều môn/kỹ năng, mỗi phần có quy tắc riêng.
3. Theo kỳ thi: chọn một cấu hình chuẩn do Admin định nghĩa.

## 8. Các màn hình cần có

Tái sử dụng navigation và design system hiện tại. Ngôn ngữ UI mặc định tiếng Việt nếu dự án đang dùng tiếng Việt.

### Teacher
1. Dashboard giáo viên: tổng quan tài liệu, ngân hàng câu hỏi, đề thi và trạng thái xử lý.
2. Import dữ liệu: upload/drag-drop, chọn loại file, chọn hoặc để AI nhận diện kỳ thi, lịch sử import.
3. Chi tiết import/review: tiến trình, preview file, danh sách câu hỏi trích xuất, metadata đề xuất, confidence/review flags, chỉnh sửa, duyệt hàng loạt.
4. Ngân hàng câu hỏi: tìm kiếm, lọc theo kỳ thi/môn/lớp/kỹ năng/band/độ khó/chủ đề/trạng thái/nguồn.
5. Chi tiết câu hỏi: nội dung, lựa chọn, đáp án, giải thích, passage/audio, nguồn file và trang/câu gốc.
6. Tạo đề: chọn cấu hình, xem số câu khả dụng, số câu cần, phân bố độ khó, thời gian và preview.
7. Quản lý đề của tôi: nháp, đã phát hành, lưu trữ; xem trước, sửa, sao chép theo quyền.

### Admin
1. Dashboard quản trị.
2. Danh mục kỳ thi/môn/lớp/kỹ năng/dải điểm.
3. Cấu hình đề đơn.
4. Cấu hình combo và các section.
5. Quy tắc chọn câu, chấm điểm và thời gian.
6. Quản lý người dùng/quyền nếu thuộc phạm vi dự án.
7. Theo dõi import, dữ liệu lỗi và trạng thái xử lý AI.

### Student
1. Danh sách đề thi và bộ đề.
2. Chi tiết đề trước khi bắt đầu.
3. Màn hình làm bài với timer, điều hướng câu, lưu tiến độ và nộp bài.
4. Kết quả/giải thích theo chính sách cấu hình.
5. Thành phần chuyên biệt: audio player, passage, writing editor, speaking recorder tùy loại câu.

## 9. Gợi ý mô hình dữ liệu

Đây là mô hình logic; điều chỉnh theo ORM/schema đang có, không nhất thiết phải dùng nguyên tên.

### SourceDocument
- id
- original_filename
- file_type
- storage_key
- uploaded_by
- uploaded_at
- detected_exam_type
- confirmed_exam_type
- processing_status: UPLOADED / PROCESSING / NEEDS_REVIEW / APPROVED / FAILED
- extraction_metadata
- error_message

### Question
- id
- source_document_id (nullable với câu AI tạo mới)
- source_reference (page/section/question number)
- origin_type: EXTRACTED / AI_GENERATED / MANUAL
- exam_type
- subject
- grade_level
- skill
- target_score_min / target_score_max
- target_band_min / target_band_max
- question_type
- difficulty
- topic
- question_text
- options (structured data hoặc bảng liên quan theo convention hiện tại)
- correct_answer
- explanation
- passage_id / audio_asset_id / group_id
- status: DRAFT / NEEDS_REVIEW / APPROVED / REJECTED / ARCHIVED
- created_by, reviewed_by, reviewed_at

### ExamConfiguration
- id, name, code, exam_type, description, active
- grade/subject/skill/target range
- duration, question_count
- section rules, difficulty distribution, scoring rules
- created_by, updated_by, timestamps

### ExamBundle / ExamSection
- bundle metadata
- sections ordered by position
- each section references a configuration and/or rules
- section-specific duration, question count, scoring rules

### Exam / ExamQuestion
- exam metadata, creator, configuration snapshot, status
- ExamQuestion stores selected question reference and an immutable snapshot of rendered content/answers as needed
- do not expose correct answers to the student before submission unless the policy explicitly allows it.

### ImportJob (nếu cần tách khỏi SourceDocument)
- source_document_id
- status/progress
- extracted counts
- warning/error counts
- started_at/completed_at
- processing version

## 10. Chất lượng dữ liệu và an toàn

- Kiểm tra file type và dung lượng ở backend; không tin MIME type do client gửi.
- Giới hạn quyền xem/tải file và câu hỏi theo vai trò/phạm vi.
- Không trả đáp án đúng trong API làm bài trước khi học sinh nộp.
- Chống duplicate import có chiến lược rõ ràng (hash file, external_id, nội dung chuẩn hóa hoặc cảnh báo để người dùng quyết định).
- Bảo toàn passage/audio và quan hệ nhóm câu hỏi.
- Không tự suy đoán đáp án nếu tài liệu không có đáp án; đánh dấu NEEDS_REVIEW.
- Ghi log lỗi xử lý nhưng không log secrets hoặc dữ liệu nhạy cảm không cần thiết.
- AI output phải được validate bằng schema và kiểm tra các trường bắt buộc.
- Có thể retry tác vụ import thất bại, không tạo dữ liệu trùng sau retry.
- Có audit trail cho việc chỉnh sửa/duyệt nếu phù hợp với kiến trúc hiện tại.
- Không cho phép đề phát hành tham chiếu tới câu hỏi chưa được duyệt, trừ khi Admin chủ động cấu hình một workflow khác.

## 11. API/service gợi ý

Chỉ tạo hoặc điều chỉnh endpoint theo convention dự án; các route dưới đây là gợi ý, không phải yêu cầu bắt buộc.

- POST /api/imports — tạo import job và tải/đăng ký file.
- GET /api/imports — danh sách import.
- GET /api/imports/{id} — trạng thái và kết quả phân tích.
- POST /api/imports/{id}/retry — chạy lại import.
- POST /api/imports/{id}/review — xác nhận metadata/kết quả.
- GET /api/questions — tìm kiếm/lọc ngân hàng câu hỏi.
- POST /api/questions — tạo câu hỏi thủ công.
- PATCH /api/questions/{id} — chỉnh sửa.
- POST /api/questions/{id}/approve — duyệt câu hỏi.
- GET/POST/PATCH /api/exam-configurations — quản lý cấu hình.
- GET/POST/PATCH /api/exam-bundles — quản lý combo.
- POST /api/exams/generate — tạo đề theo cấu hình.
- GET /api/exams/{id} — xem đề theo quyền.
- POST /api/exams/{id}/publish — phát hành đề.
- GET /api/student/exams — danh sách đề học sinh được phép làm.
- POST /api/exams/{id}/attempts — bắt đầu làm bài.
- PATCH /api/attempts/{id}/answers — lưu câu trả lời.
- POST /api/attempts/{id}/submit — nộp bài.

## 12. Acceptance criteria

### Import và AI
- Người dùng có quyền có thể upload định dạng được hỗ trợ và thấy trạng thái xử lý.
- Có preview và lỗi theo dòng/câu khi trích xuất không hoàn chỉnh.
- AI đề xuất phân loại và không tự đánh dấu đã xác nhận.
- Giáo viên sửa/duyệt kết quả trước khi dùng trong đề.
- Có thể truy xuất file gốc và vị trí nguồn của câu hỏi.
- Import lỗi có thể retry và không tạo duplicate ngoài ý muốn.

### Admin
- Admin tạo/sửa/tắt cấu hình đề đơn.
- Admin tạo combo với nhiều section và quy tắc riêng.
- Admin chỉnh sửa môn theo lớp; không hard-code danh sách minh họa.
- Admin cấu hình band/dải điểm và kỹ năng cho IELTS/TOEIC.

### Sinh đề
- Chỉ dùng câu hỏi phù hợp và đã duyệt theo mặc định.
- Kiểm tra đủ câu trước khi tạo đề.
- Báo thiếu dữ liệu rõ ràng; không sinh đề giả vờ đạt chuẩn.
- Passage/audio và các câu phụ thuộc được giữ nguyên nhóm.
- Đề đã phát hành có snapshot để tránh thay đổi ngoài ý muốn.

### Học sinh
- Giao diện render phù hợp từng dạng câu hỏi/kỹ năng.
- Timer, chuyển câu, đánh dấu, lưu câu trả lời và nộp bài hoạt động theo backend.
- Đáp án không bị lộ trước thời điểm cho phép.

### Chất lượng
- Có loading, error, empty, success states.
- Phân quyền được kiểm tra ở backend.
- Có validation phía client và server.
- Test các rule sinh đề, phân quyền và dữ liệu thiếu nếu test framework có sẵn.

## 13. Thứ tự triển khai đề xuất

### Phase 1 — Nền tảng dữ liệu
- Khảo sát repo.
- Danh mục kỳ thi/môn/lớp/kỹ năng.
- Import Excel/CSV theo template.
- CRUD và review ngân hàng câu hỏi.
- Cấu hình đề đơn/combo và tạo đề từ câu hỏi đã duyệt.

### Phase 2 — Trích xuất file và AI
- PDF/DOCX/OCR.
- AI trích xuất câu hỏi có cấu trúc.
- Phân loại và confidence/review flags.
- Màn hình review/approve.

### Phase 3 — Trải nghiệm làm bài
- Student exam list, exam runner, save progress, submit, scoring.
- Render dạng câu hỏi chuyên biệt.
- Kết quả và giải thích.

### Phase 4 — Hoàn thiện
- Báo cáo import, audit trail, chống trùng, retry.
- Kiểm thử, xử lý lỗi và tối ưu hiệu năng.
- Tinh chỉnh UI nhất quán với design system hiện có.

## 14. Prompt thực thi cho Codex

Hãy đọc toàn bộ repository hiện tại và triển khai tính năng AI Exam Generator theo tài liệu này, tích hợp vào ứng dụng hiện có. Bắt đầu bằng việc xác định stack, cấu trúc, các module liên quan, cách routing, auth/role, database và design system. Sau đó nêu kế hoạch ngắn gọn và triển khai theo từng phase nhỏ.

Ưu tiên Phase 1 trước: ngân hàng câu hỏi có cấu trúc, import Excel/CSV, review/approve, cấu hình đề đơn/combo và sinh đề từ câu hỏi đã duyệt. Tiếp theo mới triển khai import PDF/DOCX/OCR và tích hợp AI theo abstraction phù hợp với stack hiện có.

Không tự ý viết lại dự án, không thay đổi framework, không hard-code ví dụ cấu hình thành quy tắc cố định, không dùng dữ liệu giả như dữ liệu thật, không tuyên bố AI hoạt động nếu chưa tích hợp model/API thật. Dùng lại component và style system hiện có. Bảo đảm phân quyền, validation, loading/error/empty states, test và migration cần thiết. Khi hoàn tất, báo cáo file đã thay đổi, tính năng đã triển khai, cách chạy/test, cấu hình môi trường còn thiếu và các giới hạn còn lại.
