# Endpoint Security Matrix

Derived from Spring controller mappings and `SecurityConfig`. The roles below mean an authenticated principal with exactly `ROLE_USER` or `ROLE_ADMIN`. Unauthenticated protected requests should receive 401; authenticated requests with an insufficient role should receive 403. For owner-scoped records, a different user's ID must not return or mutate that record (most service paths return 404 for missing/foreign rows; attempt and analytics guards return 403).

| Method and endpoint | Allowed role | Ownership / entitlement | Expected outcome and current control |
|---|---|---|---|
| `POST /api/v1/auth/login`, `/google`, `/signup`, `/signup/verify`, `/signup/resend`; `GET /api/v1/auth/google/config` | Public | Login credentials, verified Google ID token, email verification code | Public only at these exact auth paths; invalid credentials/token/code rejected. Verification code remains hashed in persistence. |
| `GET /api/v1/auth/profile` | USER, ADMIN | Current principal | Protected by default; returns caller profile only. |
| `GET /api/v1/user/users` | USER, ADMIN | None | Protected `/api/v1/user/**` path; wrong role denied. |
| `GET /api/v1/certificates`, `GET /api/v1/certificates/{id}` | USER, ADMIN | Catalog | Authenticated reads. |
| `POST /api/v1/certificates`, `PUT /api/v1/certificates/{id}`, `DELETE /api/v1/certificates/{id}` | ADMIN | Global catalog | Method-level `hasRole('ADMIN')`. |
| `GET /api/v1/topics`, `/certificate/{id}`, `/certificate/{id}/tree`, `/type/{type}`, `/{id}`, `/{id}/children` | USER, ADMIN | Catalog | Authenticated reads. |
| `POST /api/v1/topics`, `PUT /api/v1/topics/{id}`, `DELETE /api/v1/topics/{id}` | ADMIN | Global catalog | Method-level `hasRole('ADMIN')`. |
| `GET /api/v1/questions/random`, `GET /api/v1/questions` | ADMIN | Global question bank | Admin-only via authority check. |
| `GET /api/v1/questions/{id}` | USER, ADMIN | USER only sees approved question; answer/explanation removed | Controller enforces published-state visibility for USER. |
| `POST /api/v1/questions`, `PUT /api/v1/questions/{id}`, `POST /api/v1/questions/{id}/submit-review`, `/approve`, `/reject` | ADMIN | Global question bank | Admin-only; routes use role/authority checks. |
| `GET /api/v1/exams`, `GET /api/v1/exams/{id}`, `/detail` | USER, ADMIN | USER sees only visible/available exams; ADMIN sees all | Controller passes current identity and admin state to visibility service. |
| `DELETE /api/v1/exams/{id}`, `POST /api/v1/exams/{id}/archive` | ADMIN | Global exams | Admin-only. |
| `GET /api/v1/exam-templates` | USER, ADMIN | Active templates | Authenticated. |
| `POST /api/v1/exam-templates`, `DELETE /api/v1/exam-templates/{id}` | ADMIN | Global templates | Admin-only. |
| `POST /api/v1/exam-templates/{id}/generate` | USER, ADMIN | Generated exam belongs to USER; ADMIN may generate without owner | Controller derives owner from authenticated role, never accepts owner ID from request. |
| `POST /api/v1/attempts/start`, `GET /api/v1/attempts/me/history` | USER, ADMIN | Current user | Owner ID is derived from the current principal. |
| `GET /api/v1/attempts/{id}`, `GET /result` | USER owner or ADMIN | Attempt owner | Owner check; foreign USER denied. |
| `PATCH /api/v1/attempts/{id}/answers`, `POST /submit` | USER owner | Attempt owner | Service receives current user ID and enforces ownership. |
| `POST /api/v1/attempts/{id}/violations` | USER owner or ADMIN | Attempt owner | Controller verifies attempt owner before recording. |
| `POST /api/v1/analytics/attempts/{id}/analyze`, `GET /analysis` | USER owner or ADMIN | Attempt owner | Service verifies attempt owner before read/write. |
| `GET /api/v1/analytics/me/progress`, `/me/recommendations`; `POST /recommendations/{id}/start`, `/complete` | USER current user; ADMIN override for ID-based recommendations | Current user | Current-user ID used; recommendation mutations verify owner. |
| `GET /api/v1/adaptive/profile`, `/recommend`, `/forecast`; `POST /attempts/{id}/update` | USER, ADMIN | Current user's profile and attempts | Controller uses current principal; update service validates attempt ownership. |
| `POST /api/v1/ai/grade`, `GET /evaluations/{id}`, `/evaluations/attempt/{id}` | USER owner with AI entitlement; ADMIN with AI entitlement | Attempt owner; ADMIN override | Service validates attempt owner before grading/read. |
| `GET /api/v1/ai/jobs`, `POST /jobs/{id}/retry`, `POST /rubrics`, `PUT /rubrics/{id}` | ADMIN | Global AI operations | Admin-only. |
| `GET /api/v1/ai/rubrics/certificate/{id}` | USER, ADMIN | Shared rubric catalog | Authenticated read. |
| `POST /api/v1/content-generation`, `GET /content-generation`, `GET /{id}`, `POST /{id}/apply` | ADMIN | Current admin-created generation; apply admin-only | Admin-only; service scopes user-facing listing by current admin ID. |
| `GET /api/v1/admin/users`, `/analytics`, `/{userId}/attempts`, `/{userId}/progress`; `POST /api/v1/admin/users`; `PUT /{userId}/role`; `PATCH /{userId}/status` | ADMIN | Global user administration | `/api/v1/admin/**` requires `ROLE_ADMIN`; integration-tested. |
| `POST /api/v1/tutor/conversations`, `GET /conversations`, `GET/POST /conversations/{id}/messages`, `DELETE /conversations/{id}` | USER current owner with AI entitlement; ADMIN uses own scope | Conversation owner | Service checks conversation owner for ID-based operations. |
| `POST /api/v1/rag/documents`, `GET /documents`, `GET /documents/{id}`, `/search`, `DELETE /documents/{id}` | USER current owner; ADMIN uses own scope | Document owner | Service uses owner-scoped document lookup. |
| `POST /api/v1/admin/knowledge-base`, `/pdf`; `GET /admin/knowledge-base`, `/{id}`; `PUT /{id}`, `PATCH /{id}/active`, `DELETE /{id}` | ADMIN | Shared knowledge corpus | Controller-level ADMIN and `/api/v1/admin/**` protection. |
| `POST /api/v1/plans`, `GET /plans`, `GET /{id}`, `/{id}/summary`, `POST /{id}/complete`, `/archive` | USER current owner; ADMIN uses own scope | Study plan owner | Plan lookup is owner-scoped. |
| `PATCH /api/v1/plans/tasks/{taskId}` | USER owning task's plan | Plan owner | Service resolves task's plan and checks its owner before update. |
| `POST /api/v1/speaking/submissions`, `GET /submissions/{id}`, `/submissions/attempt/{attemptId}`, `POST /evaluations`, `GET /evaluations/{id}`, `/evaluations/submission/{submissionId}` | USER owning attempt/submission with speaking entitlement | Attempt/submission owner | Submission verifies attempt ownership; reads/evaluation verify submission owner. |
| `GET /api/v1/practice`, `GET /{id}` | USER, ADMIN | Shared practice catalog | Authenticated reads. |
| `POST /api/v1/practice`, `PUT /{id}`, `DELETE /{id}` | ADMIN | Shared catalog | Admin-only. |
| `POST /api/v1/payments/checkout`, `GET /payments/{id}`, `/payments/history`, `GET /api/v1/me/entitlements` | USER, ADMIN | Current user | Payment lookups constrain both payment ID and user ID. |
| `GET /api/v1/payments/paypal/return` | Public provider return | Unpredictable provider order token; capture is provider-verified | Public redirect route; it must not return payment details and final status remains webhook/verification controlled. |
| `POST /api/webhooks/stripe`, `/api/webhooks/paypal` | Public provider callbacks | Valid provider signature/event | Public in filter chain; strategy must verify signature before state changes. |
| `GET /swagger-ui/**`, `/v3/api-docs/**`, `/api-docs/**` | Public | None | Public API documentation. |

## Coverage limits

The integration tests exercise anonymous, USER, and ADMIN behavior on representative protected routes. Service tests cover foreign attempt, AI evaluation, and planner task ownership. The matrix is a source-derived policy, not proof that every listed route has an HTTP integration test or that external payment webhook signatures were tested against live provider traffic. Repeat the route matrix when adding controller mappings.
