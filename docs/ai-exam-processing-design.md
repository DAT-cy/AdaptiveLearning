# Thiết kế hệ thống AI xử lý và sinh đề thi

## 1. Mục tiêu và phạm vi

Đây là thiết kế đã được thống nhất và được triển khai theo từng phase. Phase 1 hiện đang bổ sung lưu file đề gốc có metadata/checksum và endpoint preview có phân quyền; cấu trúc document/resource/group và generator-aware còn là các phase tiếp theo.

Các nguyên tắc chính:

- PDF gốc là nguồn đối chiếu bất biến; nội dung OCR và dữ liệu đã biên tập là các lớp riêng.
- Không suy diễn đáp án khi không có khóa đáp án. Câu trả lời nhận diện từ nguồn phải có xuất xứ và trạng thái duyệt.
- Câu hỏi có thể độc lập hoặc liên kết với một hay nhiều tài nguyên; tài nguyên có thể được nhiều câu hỏi dùng chung.
- Khi sinh đề ngẫu nhiên, nhóm câu hỏi phụ thuộc vào cùng tài nguyên là đơn vị lựa chọn nguyên tử.
- Đề AI sinh mới là bản nháp có nguồn dẫn và kiểm định riêng; không ghi đè câu hỏi, đáp án hay tài liệu gốc.

## 2. Kiến trúc hiện tại

### Import và OCR

`ExamImportController` nhận PDF hoặc văn bản, còn `ExamImportAnalysisJobService` chạy phân tích bất đồng bộ trong tiến trình backend. Hàng đợi có giới hạn và trạng thái job lưu trong bộ nhớ, nên job mất khi backend khởi động lại. Luồng chính nằm trong `ExamImportServiceImpl`:

1. PDF có lớp văn bản được đọc bằng PDFBox rồi chia đoạn văn bản.
2. PDF scan được chia theo trang và gửi qua `AiProvider.analyzeDocumentSync`; kết quả trùng được loại bằng số câu, nội dung và lựa chọn.
3. AI trả về một mảng câu hỏi phẳng. Các trường hiện có gồm nội dung, lựa chọn, đáp án, giải thích, topic, số câu nguồn và điểm.
4. Khóa đáp án có thể được gửi sau. AI ánh xạ theo từng lô tối đa 20 câu, nhưng không trả về confidence hay bằng chứng cụ thể cho từng liên kết.

Trước phase 1, bản ghi `ExamImport` chỉ lưu `sourceContent`, `answerContent`, `questionsJson` và trạng thái. Phase 1 đang bổ sung storage key, tên/MIME, kích thước và SHA-256; PDF có thể được tải có phân quyền và xem cạnh form semantic. Ảnh trang, tọa độ vùng, cấu trúc sections/resources/groups và highlight tương ứng chưa được lưu. PDF có text layer vẫn đi qua trích xuất text, không giữ lại layout; vì vậy hiện chưa thể tái tạo tài liệu nhiều trang gần với PDF.

### Ngân hàng câu hỏi và sinh đề

Khi duyệt import, mỗi câu được chuyển thành một `Question` riêng. `contentJson` hiện chỉ chứa metadata import và số thứ tự; `optionsJson` và đáp án là dữ liệu JSON/text. Không có liên kết chuẩn từ câu hỏi tới passage, audio, ảnh hoặc bảng.

`ExamTemplate` và `ExamTemplateSection` định nghĩa các phần theo topic, loại câu, số câu và điểm. `ExamTemplateServiceImpl` chọn ngẫu nhiên các câu APPROVED khớp certificate/topic/type/difficulty; người dùng cũng có thể gửi ID câu đã chọn. Không có lựa chọn theo Question Group hay ràng buộc tài nguyên. `ExamQuestion.sectionName` chỉ là tên phần dạng text.

Đường `ContentGenerationServiceImpl` là luồng riêng: truy xuất đoạn kiến thức từ RAG để AI sinh câu hỏi mới cho admin duyệt. Đây không phải luồng tạo đề cho học sinh từ câu hỏi đã import. RAG lưu văn bản và chunks, phù hợp tìm kiếm tri thức nhưng không thay thế kho tài liệu đề thi có trang, tọa độ, ảnh và quan hệ nguồn.

Giao diện làm bài hiển thị `question.content` và các lựa chọn; chưa render `contentJson` thành nội dung phong phú. Do đó dù JSON có tham chiếu media, người học hiện cũng không nhận được tài nguyên đó.

## 3. Luồng nghiệp vụ đích

```text
PDF đề ──> lưu file bất biến ──> OCR / document understanding ──> bản nháp có cấu trúc
                                                               │
                                                               ├─ Trang / vùng / section
                                                               ├─ Resource: passage, ảnh, bảng...
                                                               ├─ Question / Question Group
                                                               └─ confidence + nguồn bằng chứng
                                                                      │
PDF đáp án tùy chọn ──> nhận diện ──> ghép ứng viên + bằng chứng ──> giáo viên duyệt
                                                                      │
                                              giáo viên rà soát cấu trúc và nội dung
                                                                      │
                                                 publish nguyên tử vào ngân hàng
                                                                      │
                                    random theo group hoặc AI tạo biến thể có provenance
```

PDF đề không chứa đáp án phải tạo câu hỏi với đáp án rỗng. Không bật chế độ giải đề bằng AI trong import mặc định. Nếu đáp án ở chính PDF, đây phải là lựa chọn rõ ràng; nếu không có đáp án, UI cho phép bổ sung file khóa đáp án sau. Trạng thái nhận diện, confidence và trạng thái duyệt cần tách biệt với việc câu hỏi có đáp án hay không.

## 4. Mô hình dữ liệu đề xuất

Giữ `Question` làm ngân hàng câu hỏi đã duyệt để giảm ảnh hưởng tới attempt và phân tích hiện có. Thêm các bảng quan hệ thay vì nhồi toàn bộ cấu trúc vào JSON:

| Thực thể | Trách nhiệm và quan hệ |
| --- | --- |
| `ExamDocument` | Bản nhập tài liệu; certificate, uploader, title, file key, MIME, kích thước, checksum, số trang, trạng thái xử lý/duyệt, phiên bản và OCR metadata. Một document có nhiều sections, resources và staging questions. |
| `ExamSection` | Phần có thứ tự và cấu trúc lồng nhau tùy nhu cầu; ví dụ Reading, Listening, Writing. Có `document_id`, `parent_section_id`, thứ tự và loại phần. |
| `ContentResource` | Passage, audio, image, chart, table, scenario hoặc asset khác; lưu nội dung có cấu trúc, asset key, nguồn, trang/vùng, thứ tự và metadata. Thuộc document/section. |
| `QuestionGroup` | Tập câu hỏi cùng ngữ cảnh, tùy chọn tham chiếu resource mặc định và quy tắc chọn. Nhóm là đơn vị chọn nguyên tử khi random; câu độc lập không cần group. |
| `QuestionResource` | Bảng nối nhiều-nhiều giữa câu hỏi và resource, có role, thứ tự, vùng/thời điểm tham chiếu và confidence. Cho phép một câu cần nhiều resource và một resource dùng cho nhiều câu. |
| `ExamQuestionSource` | Nối Question đã duyệt với document/section/group và số câu/trang/vùng nguồn; hỗ trợ traceability, không đồng nhất Question với một lần import. |
| `AnswerKeyMatch` | Ứng viên đáp án, question link, confidence, bằng chứng (trang/văn bản/vị trí), trạng thái PENDING/ACCEPTED/REJECTED và người duyệt. Chỉ khi duyệt mới cập nhật đáp án chuẩn của Question. |
| `GeneratedExamRun` (giai đoạn sau) | Chế độ sinh, blueprint/version, nguồn đầu vào, model/prompt version, validation result và trạng thái duyệt cho đề AI mới. |

Giai đoạn đầu nên lưu dữ liệu staging liên quan tới import riêng; chỉ tạo `Question` và các link nguồn trong cùng transaction khi giáo viên xác nhận. Không để câu chưa duyệt tham gia random. Bản PDF và asset nên lưu trong object/file storage qua một `FileStorage` abstraction; DB giữ key, checksum và metadata, không giữ blob lớn trong cột JSON.

Không cần thêm entity riêng cho `AnswerKey` nếu mỗi lần import chỉ có một khóa đáp án. `AnswerKeyMatch` đủ để quản lý từng mapping và review. Nếu sau này có nhiều phiên bản/loại khóa, bổ sung `AnswerKeyDocument` làm cha.

### Quyết định lưu trữ file

Giai đoạn đầu dùng `FileStorage` abstraction với filesystem storage trên persistent Docker volume (`/data/exam-assets`), lưu file bằng key ngẫu nhiên và metadata/checksum trong DB. Không lưu file trong workspace/container layer hoặc dưới `public`. Đây là lựa chọn phù hợp với local Compose hiện tại và dễ backup/khôi phục cùng volume. Giữ interface để đổi sang S3-compatible storage khi có môi trường production/multi-instance; không ràng buộc entity/API vào filesystem path.

## 5. Import, render và answer-key matching

### Document understanding

- Lưu file gốc trước khi gọi AI, tạo checksum để nhận diện upload trùng và khôi phục job.
- Chuẩn hóa output của provider thành schema versioned: pages, blocks, sections, resources, groups, questions, source references và confidence.
- Mỗi block giữ `pageNumber`, thứ tự đọc và bounding box chuẩn hóa (0..1). Cần ghi rõ OCR/layout provider và phiên bản model.
- PDF text-layer vẫn giữ PDF gốc; dùng text extraction làm tín hiệu bổ sung, không thay layout/source image.
- PDF scan xử lý theo trang/nhóm trang. Queue/job nên bền vững qua DB hoặc hàng đợi hiện có trước khi tăng quy mô; trạng thái job và từng bước cần idempotent.
- Lỗi parse/low confidence tạo item chờ review, không tự nối sai resource hay đáp án.

### Render

Không nên hứa HTML tái tạo chính xác pixel của mọi PDF bằng OCR. PDF có thể chứa scan, công thức, bảng phức tạp và font/layout không thể suy ra đầy đủ từ text.

Thiết kế UI có hai chế độ đồng bộ:

1. PDF gốc hiển thị bằng viewer theo trang, là nguồn chuẩn để đối chiếu.
2. Bản tái tạo semantic hiển thị sections, resource blocks và câu hỏi có thể chỉnh sửa; chọn block sẽ highlight vùng tương ứng trên PDF.

Nếu yêu cầu fidelity cao cho đề lưu trữ, dùng trang PDF gốc trong trải nghiệm thi có kiểm soát. Bản semantic là nguồn cho random/biến thể. Mỗi block cần order và nguồn vị trí; bảng/công thức/ảnh không được chuyển thành văn bản giả định khi AI không đọc chắc chắn.

### Ghép đáp án

AI nhận cả danh sách câu đã trích xuất và file khóa đáp án. Ghép candidate theo số câu nguồn, nội dung, loại câu, lựa chọn và ngữ cảnh; không dùng vị trí đơn lẻ. Mỗi candidate trả về đáp án, nguồn bằng chứng và confidence. Quy tắc ban đầu:

- Confidence cao và bằng chứng duy nhất: đề xuất chấp nhận nhanh, vẫn lưu provenance.
- Confidence trung bình/thấp, đáp án mâu thuẫn hoặc câu không ghép được: bắt buộc giáo viên duyệt.
- MCQ chỉ chấp nhận label tồn tại trong options; không khớp thì để trống.
- Essay lưu sample answer, rubric và score bands riêng; không đặt sample answer thành đáp án tự chấm.
- Kết quả AI không ghi đè đáp án đã được giáo viên xác nhận nếu người dùng chưa chủ động chọn ghi đè.

## 6. Sinh đề

Tách rõ hai chế độ, có thể dùng chung blueprint nhưng không dùng chung quy tắc nguồn:

### Random từ ngân hàng

- Blueprint khai báo số câu độc lập và số Question Group theo section/topic/type/difficulty.
- Candidate query chỉ lấy câu APPROVED có đáp án hợp lệ khi bài thi cần chấm tự động.
- Lấy group trước; khi chọn group phải đưa toàn bộ câu bắt buộc và resource gắn với group vào đề. Chỉ cho subset khi group policy cho phép và subset vẫn độc lập.
- Sau đó lấy câu standalone không thuộc group. Một `selectedIds` và `selectedGroupIds` duy nhất ngăn trùng trong cùng bài.
- Validate đủ số câu, tổng điểm, duration và mọi resource bắt buộc trước khi tạo Exam. Persist snapshot nguồn/blueprint để đề cũ không đổi nếu bank được sửa.

### AI tạo biến thể

- Lấy resource và group đã duyệt làm context cố định; AI chỉ sinh biến thể dựa trên context đó.
- Output phải nêu `sourceResourceIds`, group, đáp án, giải thích/rubric và kiểm tra schema/domain.
- Lưu thành draft có source provenance; không publish thẳng cho học sinh. Chạy kiểm tra đáp án, duplicate/leakage, resource compatibility và review theo policy trước phát hành.
- Không sửa bản gốc. Đề được random từ câu cũ phải được đánh dấu khác với đề có câu AI tạo mới.

## 7. API và giao diện đề xuất

Giữ endpoint hiện tại cho compatibility trong giai đoạn chuyển tiếp, bổ sung API phiên bản hóa theo document:

- `POST /api/v1/exam-documents` nhận PDF, metadata; trả `documentId`, `jobId`.
- `GET /api/v1/exam-documents/{id}/analysis` trả trạng thái bền vững và tiến trình theo bước.
- `GET/PUT /api/v1/exam-documents/{id}/draft` đọc/lưu cấu trúc đã AI phân tích và chỉnh sửa.
- `POST /api/v1/exam-documents/{id}/answer-keys` nhận file đáp án; trả mapping candidates và confidence.
- `PUT /api/v1/exam-documents/{id}/answer-key-matches` lưu quyết định review.
- `POST /api/v1/exam-documents/{id}/confirm` validate và promote nguyên tử.
- API exam blueprint hỗ trợ resource/group requirements; API tạo đề ghi rõ `mode=RANDOM_BANK` hoặc `mode=AI_VARIANT`.

Màn import: upload, tiến trình, trạng thái cấu trúc và danh sách cảnh báo. Màn review: PDF gốc cạnh bản semantic; điều hướng trang/section/group; click câu highlight vùng nguồn; upload/thay asset; review đáp án/confidence. Màn ngân hàng: filter theo topic/difficulty/type, section/group/resource và trạng thái nguồn. Màn sinh đề: chọn blueprint, nhóm/câu độc lập, độ khó, số lượng, điểm và chế độ; preview luôn render resource cùng group.

## 8. Migration, rollout và tương thích

Migration nên additive và theo expand-migrate-contract:

1. Tạo bảng document/section/resource/group/link/match và file storage abstraction; giữ nguyên `exam_imports`, `questions`, `exams`, `exam_questions`, templates và attempt tables.
2. Backfill mỗi import hiện có thành document `LEGACY_TEXT_ONLY`; đưa `sourceContent` và `questionsJson` vào staging tương ứng. Không giả định có PDF gốc vì hiện DB không lưu file.
3. Câu đã confirm giữ nguyên `questionId`, đáp án, status và liên kết exam/attempt. Tạo source link từ metadata `examImportId`/`questionNumber` nếu dữ liệu hợp lệ; trường hợp không khớp để legacy/unlinked và báo cáo.
4. Câu cũ không có group được xem như standalone. `ExamQuestion.sectionName` và template sections cũ tiếp tục đọc được; chỉ chuyển thành group khi có bằng chứng từ nguồn.
5. Đưa API/UI mới vào đọc/ghi song song; bật theo feature flag; đo lỗi OCR/matching; sau khi xác minh mới ngừng luồng legacy. Không drop cột hoặc rewrite attempt snapshots trong đợt đầu.

Các khác biệt API nên additive: `contentJson` hiện vẫn giữ shape cũ; tài nguyên mới trả qua DTO có trường `resources`/`group` rõ schema. Không gửi `correctAnswer` tới endpoint làm bài cho học sinh trước khi nộp.

## 9. Các giai đoạn triển khai

1. **Nền tảng và hợp đồng:** chốt JSON schema/version, storage interface, confidence/review rules, migrations additive và fixtures PDF.
2. **Import có cấu trúc:** lưu PDF gốc, trang/vùng, sections/resources/groups, job bền vững; giữ luồng text cũ làm fallback.
3. **Review và answer key:** UI PDF/song song semantic, sửa block, ghép answer key có evidence/confidence, duyệt và promote.
4. **Render và random theo group:** consumer/API trả resource; cập nhật màn làm bài; generator chọn group nguyên tử và kiểm tra điểm/số lượng.
5. **AI variant:** draft generation từ context đã duyệt, provenance, validation và quy trình publish riêng.

## 10. Kiểm thử và tiêu chí nghiệm thu

- PDF có text layer, scan, nhiều trang, hai cột, passage dùng chung, bảng/ảnh/công thức, số câu tiếp nối và PDF lỗi.
- Tọa độ/block/page/order ổn định; sửa OCR không đổi PDF gốc; round-trip draft giữ resource links.
- Khóa đáp án lệch số, thiếu câu, câu trùng, answer mâu thuẫn, MCQ label không tồn tại, essay rubric và confidence thấp.
- Một resource dùng bởi nhiều câu; một câu dùng nhiều resource; câu standalone; không được random câu thuộc group mà thiếu passage/audio.
- Sinh đề đủ/thiếu inventory, không trùng câu/group, đúng tổng điểm; AI variant không mutate nguồn và không publish khi validation/review chưa đạt.
- API không lộ đáp án cho learner; quyền truy cập document/asset theo owner/role; upload kiểm tra MIME/kích thước, malware policy, retention và URL có thời hạn.
- Migration trên bản sao dữ liệu thật; giữ nguyên question IDs, exam/attempt liên kết và khả năng khởi động với schema cũ/mới trong giai đoạn rollout.

## 11. Quyết định đã thống nhất

1. Khi review, hiển thị PDF gốc song song với bản semantic và highlight trang/vùng tương ứng. Khi làm bài, render bản semantic tương tác; resource gốc vẫn được giữ/hiển thị theo đúng quan hệ để bảo toàn ngữ cảnh.
2. Không phân tích audio bằng AI ở giai đoạn đầu. Giáo viên upload audio thủ công ngay trong mục Listening (ví dụ mục I); file được liên kết với section và dùng chung cho toàn bộ câu hỏi thuộc mục đó.
3. Khi upload file đáp án, hệ thống nhận diện và điền đáp án ứng viên vào câu hỏi. Giáo viên kiểm tra/sửa kết quả, sau đó xác nhận đề.
4. Xác nhận của giáo viên là bước duyệt cuối: các câu hợp lệ được promote sang ngân hàng APPROVED ngay trong transaction. Không có hàng chờ duyệt cấp hai.
5. Giai đoạn đầu dùng filesystem storage trên persistent Docker volume phía sau `FileStorage` abstraction; production có thể thay adapter bằng S3-compatible storage mà không đổi contract/domain.

AI variant vẫn cần quyết định chính sách publish riêng khi tới phase đó; khuyến nghị bắt buộc review vì nội dung mới do AI tạo. PDF gốc + bản semantic song song là lựa chọn fidelity/review; audio được upload thủ công theo section, còn speech-to-text và AI variant được để sau.

## Trạng thái triển khai

- **Phase 1 (đang thực hiện):** filesystem storage sau abstraction, volume bền vững trong Compose, metadata/checksum cho file gốc, endpoint nguồn có quyền admin/approver, PDF preview cạnh khu vực rà soát, audio manual upload gắn theo Listening section, migration additive cho DB cũ.
- **Phase 2:** schema document/section/resource/group/source và staging questions có versioned structure; OCR output tiếp tục hỗ trợ fallback JSON cũ.
- **Phase 3:** answer-key evidence/confidence và review decisions; confirm atomically promote vào ngân hàng APPROVED theo quyết định đã thống nhất.
- **Phase 4:** render resource trong attempt và random theo group nguyên tử.
- **Phase 5:** AI variant có provenance và pipeline review độc lập.

## Ghi chú kiểm tra cấu hình

Trong worktree hiện tại, cấu hình local chứa một provider credential inline. Trước khi triển khai hoặc chia sẻ repository, nên chuyển credential sang environment/secret manager và rotate credential nếu đây là khóa đang hoạt động. Giá trị không được chép vào tài liệu hoặc log.
