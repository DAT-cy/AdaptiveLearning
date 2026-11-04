# Exam Import API

Base path: `/api/v1/exam-imports`. Authenticated requests use the existing bearer JWT.

## Upload

`POST /api/v1/exam-imports` is restricted to `ROLE_ADMIN` and consumes `multipart/form-data`:

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `title` | string | yes | Maximum 200 characters |
| `certificateId` | integer | yes | Existing certificate |
| `topicId` | integer | yes | Active topic under the certificate |
| `examFile` | PDF | yes | Maximum 20 MB; must contain extractable text |
| `answerFile` | PDF | no | Maximum 20 MB; answer key paired to this exam |

The upload is parsed synchronously and returns `201` with `status: PENDING_REVIEW`, the extracted question list, and source text. If `answerFile` is absent, `answerGenerated` is `true`; answers are AI suggestions and must be checked by an approver. PDFs without a text layer are rejected; OCR is not provided.

## Review

`GET /api/v1/exam-imports` lists pending imports with metadata. `GET /api/v1/exam-imports/{id}` returns source text and extracted questions. `PUT /api/v1/exam-imports/{id}` saves corrections with `{ "questions": [...] }`. The number of questions must remain unchanged; question content, type, difficulty, options, answer, explanation, and points may be corrected.

These review operations require `ROLE_ADMIN` or `EXAM_APPROVER`. The latter authority is granted only to a teacher account whose `canApproveExams` permission is enabled.

`POST /api/v1/exam-imports/{id}/confirm` confirms the saved review. It is idempotency-protected by the import status: only `PENDING_REVIEW` imports can be confirmed. Confirmation creates `APPROVED` question-bank records; those become selectable by the existing exam builder. The original source and answer text remain attached to the import record for traceability.

Question objects use `questionType` (`MULTIPLE_CHOICE`, `TRUE_FALSE`, `FILL_BLANK`, `ESSAY`), `difficulty` (`EASY`, `MEDIUM`, `HARD`), `content`, `options` (`[{"label":"A","text":"..."}]`), `correctAnswer`, `explanation`, and `points`.

Apply `src/main/resources/db/upgrade-teacher-exam-approval.sql` and `src/main/resources/db/upgrade-exam-imports.sql` before deploying to a database that is not managed by Hibernate schema update.
