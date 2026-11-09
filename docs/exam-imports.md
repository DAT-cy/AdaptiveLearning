# Exam Import API

Base path: `/api/v1/exam-imports`. Authenticated requests use the existing bearer JWT.

## Upload

`POST /api/v1/exam-imports` is restricted to `ROLE_ADMIN` and consumes `multipart/form-data`:

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `title` | string | yes | Maximum 200 characters |
| `certificateId` | integer | yes | Existing certificate |
| `topicId` | integer | no | Optional active topic; omit to classify each question independently |
| `examFile` | PDF or text | yes | PDF, Markdown, or TXT; maximum 20 MB; scanned PDFs use OCR |
| `answerFile` | PDF or text | no | Maximum 20 MB; answer key paired to this exam |
| `answerInExam` | boolean | no | Defaults to `false`. Set `true` when official answers are embedded in the exam; do not also send `answerFile` |

The upload is parsed synchronously and returns `201` with `status: PENDING_REVIEW`, the extracted question list, and source text. When `answerInExam=true`, answers are matched from the exam itself. Otherwise a supplied `answerFile` is used; when neither is supplied, `answerGenerated` is `true` and AI-suggested answers must be checked by an approver. Each question is assigned against the certificate's topic hierarchy: the major section is considered first, then the most specific matching child topic. Unresolved skills remain unassigned for manual review and must be selected before confirmation. Scanned PDFs use the configured document-analysis provider for OCR.

The admin UI performs OCR first and displays the extracted question form. An answer key may then be uploaded with `POST /api/v1/exam-imports/{id}/answers` as `multipart/form-data` field `answerFile` (PDF, Markdown, or TXT, maximum 20 MB). The response contains the updated `examImport`, `matchedAnswerCount`, and `totalQuestionCount`. The matcher updates answers only; question content, type, topic, and options from OCR remain unchanged. Unmatched answers remain blank for manual review, and the import cannot be confirmed until all questions have answers.

The admin UI uses the asynchronous analysis flow to report real chunk progress. `POST /api/v1/exam-imports/analysis` accepts the same multipart fields and returns `202` with `{ jobId, status, progress, completedChunks, totalChunks, message }`. Poll `GET /api/v1/exam-imports/analysis/{jobId}` until `status` is `COMPLETED` or `FAILED`; completed jobs include `importId`, which can be opened with `GET /api/v1/exam-imports/{importId}`. Progress belongs to the admin who started the job. Jobs run in a bounded in-process queue and their status expires after three hours; they are not durable across backend restarts.

The original upload is stored on the persistent `EXAM_ASSET_STORAGE_PATH` volume. `GET /api/v1/exam-imports/{id}/source` serves it to authorized reviewers. For a Listening section, upload a shared audio resource with `POST /api/v1/exam-imports/{id}/resources/audio?sectionKey={key}&sectionTitle={title}` and multipart field `audioFile` (MP3, WAV, M4A, OGG, or AAC, maximum 50 MB). Upload a supporting image with `POST /api/v1/exam-imports/{id}/resources/image` using the same section query parameters and multipart field `imageFile` (PNG, JPG, JPEG, or WEBP, maximum 10 MB). The import detail response includes its section resources. Stream an asset with `GET /api/v1/exam-imports/{id}/resources/{resourceId}/file`. Resources are linked to their section, not duplicated across its questions; phase 1 does not transcribe audio or automatically extract embedded images from PDF.

Analysis splits text into chunks of at most about 10,000 characters and scanned PDFs into one page per request without overlapping pages. Repeated OCR results are deduplicated using the source number, content, and choices. The service does not impose the previous 100-question import cap. Each part is requested and parsed independently, then all question lists are combined before the import is saved. The original synchronous `POST /api/v1/exam-imports` remains available for existing clients.

## Review

`GET /api/v1/exam-imports` lists pending imports with metadata. `GET /api/v1/exam-imports/{id}` returns source text and extracted questions. `PUT /api/v1/exam-imports/{id}` saves corrections with `{ "questions": [...] }`. The number of questions must remain unchanged; question content, type, difficulty, options, answer, explanation, and points may be corrected.

These review operations require `ROLE_ADMIN` or `EXAM_APPROVER`. The latter authority is granted only to a teacher account whose `canApproveExams` permission is enabled.

`POST /api/v1/exam-imports/{id}/confirm` confirms the saved review. It is idempotency-protected by the import status: only `PENDING_REVIEW` imports can be confirmed. Confirmation creates `APPROVED` question-bank records; those become selectable by the existing exam builder. The original source and answer text remain attached to the import record for traceability.

`DELETE /api/v1/exam-imports/{id}` permanently deletes a pending import only when the authenticated caller is its uploader. Other users' imports and already-confirmed imports are not deletable through this endpoint; they return `404`. Pending-list and detail responses include `canDelete`, computed by the server for the current caller, so clients can hide the action without treating the client check as authorization.

Question objects use `questionType` (`MULTIPLE_CHOICE`, `TRUE_FALSE`, `FILL_BLANK`, `ESSAY`), `difficulty` (`EASY`, `MEDIUM`, `HARD`), `topicId` (the matched topic in the major-section hierarchy, or `null` for manual assignment), `skill` (topic name/code), `sourceQuestionNumber` (original question number where available), `sourceSectionOrder`, `sourceSectionTitle`, `sourceSectionQuestionStart`/`sourceSectionQuestionEnd`, `sectionInstructions`, `stimulusContent`, `sourcePageNumber`, `content`, `options` (`[{"label":"A","text":"..."}]`), `correctAnswer`, `explanation`, and `points`. Section metadata repeats on each question so questions 1-10, for example, remain grouped under their original section and prompt.

For a new database, apply `src/main/resources/db/upgrade-teacher-exam-approval.sql` and `src/main/resources/db/upgrade-exam-imports.sql`. Existing databases that already have `exam_imports` must also apply `src/main/resources/db/upgrade-exam-import-source-files.sql` to add original-file metadata. Apply `src/main/resources/db/upgrade-exam-import-resources.sql` to add section audio and image resources. Keep the `EXAM_ASSET_STORAGE_PATH` persistent volume backed up with the database; DB rows store storage keys, not file bytes.
