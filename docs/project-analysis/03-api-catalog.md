# API Catalog

Base prefix is `/api/v1` unless noted. Most controllers return `DefaultRes<T>`. `SecurityConfig` permits auth, Swagger, public, webhook paths and PayPal return; other paths require authentication by default. Role matching also uses `/api/v1/admin/**` and `/api/v1/user/**`; some controllers apply method-level checks or ownership checks. Confirm exact authorization per endpoint before exposing new data.

| Method | Endpoint | Controller/service | Purpose and data | Auth/status |
|---|---|---|---|---|
| POST | `/auth/signup` | `AuthController` / `UserServiceImpl` | Create unverified user and issue verification | Public; verified in code |
| POST | `/auth/signup/verify` | `AuthController` / `EmailVerificationService` | Verify email/code | Public; verified in code |
| POST | `/auth/login` | `AuthController` / `UserServiceImpl` | Credentials to JWT/role | Public; verified in code |
| POST | `/auth/google` | `AuthController` / `UserServiceImpl` | Google ID token to application JWT | Public; provider config needed |
| GET | `/auth/profile` | `AuthController` / user service | Current profile | Auth required |
| GET/POST/PUT/DELETE | `/certificates[/{id}]` | `CertificateController` | Browse/manage certificates | Auth default; exact role policy inspect |
| GET/POST | `/topics...` | `TopicController` | Topic catalog/tree | Auth default |
| GET/POST/PUT | `/questions...` | `QuestionController` | Question bank and review workflow | Auth default; admin actions enforced in controller/service |
| GET/DELETE/POST | `/exams...` | `ExamController` | List/detail/archive/delete exams | Auth default |
| GET/POST/DELETE/POST | `/exam-templates...` | `ExamTemplateController` | Templates and generate exam | Auth default |
| POST | `/attempts/start` | `AttemptController` / `AttemptServiceImpl` | Consume quota and create attempt | Auth + current user |
| GET/PATCH/POST | `/attempts/{id}`, `/{id}/answers`, `/{id}/submit`, `/{id}/result` | `AttemptController` | Read/save/submit attempt and retrieve result | Auth + owner/admin checks |
| POST | `/attempts/{id}/violations` | `AttemptController` | Record violation | Auth required |
| GET | `/attempts/me/history` | `AttemptController` | Current user's attempt history | Auth required |
| POST/GET | `/analytics/attempts/{id}/analyze`, `/analysis` | `AnalyticsController` | Create/read gap analysis | Auth default; ownership behavior verify |
| GET | `/analytics/me/progress`, `/me/recommendations` | `AnalyticsController` | Current learner analytics | Auth required |
| GET/POST | `/adaptive/profile`, `/adaptive/attempts/{id}/update`, `/adaptive/recommend`, `/adaptive/forecast` | `AdaptiveController` | Profile, update, recommendations, forecast | Auth; operations derive current user |
| POST/GET | `/payments/checkout`, `/payments/history`, `/payments/{id}` | `PaymentController` | Checkout/status/history | Auth and per-user lookup |
| POST | `/api/webhooks/stripe`, `/api/webhooks/paypal` | `PaymentWebhookController` | Provider event processing | Public route, signature verification in strategy |
| GET | `/me/entitlements` | `EntitlementController` | Plan, quota, skills, payment methods | Auth required |
| POST/GET/PUT | `/ai/grade`, `/ai/evaluations...`, `/ai/jobs...`, `/ai/rubrics...` | `AiController` | Submit grading job, inspect evaluation/jobs, retry jobs and manage rubrics | Grading/evaluation require premium entitlement; job/rubric mutations are admin-only |
| POST/GET | `/content-generation...` | `ContentGenerationController` | Generate, inspect, apply question content | Admin access intended; verify annotations |
| POST/GET | `/tutor/conversations...` | `TutorController` | Conversations/messages | Auth + owner checks in service |
| POST/GET | `/rag/documents...` | `RagController` | User documents and search | Auth + owner access in service |
| POST/GET | `/admin/knowledge-base...` | `AdminKnowledgeBaseController` | Admin retrieval corpus operations | Admin route |
| POST/GET | `/speaking/submissions...`, `/speaking/evaluations...` | `SpeakingController` | Speaking submission/evaluation APIs | Auth default; end-to-end AI path unverified |
| POST/GET/PATCH | `/plans...` | `PlannerController` | Plans, task update, summary, completion/archive | Auth + current-user checks vary by method |
| GET/POST/PUT/DELETE | `/practice[/{id}]` | `PracticeController` | Read practice packages; admins create/update/delete | Reads require auth; mutations require ADMIN |

The frontend service clients call several endpoints, but coverage is not exhaustive and may drift. `learningService.js` calls exams, attempts, analytics, adaptive, plans, practice and tutor; `paymentService.js` calls checkout/status/history/entitlements; `adminService.js` calls user/admin, content, question and knowledge-base routes. Validate response DTOs and auth from controllers before relying on `BUSINESS.md` sample payloads. Full per-field request/response schemas are in controller DTOs and Swagger annotations, not duplicated here.
