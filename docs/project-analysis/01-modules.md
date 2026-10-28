# Modules and Responsibilities

The backend is a package-by-feature Spring application under `Adaptive Learning/src/main/java/adaptivelearning`; modules call each other through injected services/repositories, not separate deployable services. Status means code was located, not that runtime operation was integration-tested.

| Module/package | Responsibility and key components | Main dependencies | Status |
|---|---|---|---|
| `config`, `shared`, `utils` | JWT/security filters, response/error handling, enums, current-user helpers | Spring Security, JWT | Verified in source |
| `users` | Signup, email verification, password/Google login, profile and admin user operations | MyBatis `UserMapper`, SMTP, Google token verifier | Verified in source; delivery depends on config |
| `certificates`, `topics` | Learning catalog and topic hierarchy | JPA repositories; consumed by questions/analytics | Verified in source |
| `questions`, `exams` | Question bank/review and exam/template management | JPA; billing entitlement checks | Verified in source |
| `attempts` | Start, answer persistence, submit/scoring, violations, expiry | Exams, questions, entitlements, async adaptive dispatcher | Verified in source |
| `analytics`, `practice` | Gap analysis, progress, recommendations, practice packages | Attempts, topics, exams | Verified in source |
| `adaptive` | Learner profile, heuristic recommendation/forecast, BKT request | Attempt data, Python BKT HTTP client | Verified; calibration/data readiness unverified |
| `ai`, `contentgen`, `tutor` | Rubric grading/jobs, generated content, conversations/messages | AI provider, JPA | Partial; see flow notes |
| `rag` | Document/chunk storage, text/PDF extraction and search; admin knowledge base | JPA, PDFBox | Source/tests found; retrieval quality/runtime unverified |
| `speaking` | Speaking submission/evaluation endpoints and persistence | AI/business path must be checked per method | Partial; do not infer audio/STT support |
| `planner` | Study plans, tasks, summary and status changes | JPA, current user | Source found; AI-based generation not established |
| `billing` | Checkout, payment status/history, verified webhook processing and entitlements | Stripe/PayPal APIs, user persistence | Unit tests found; provider sandbox/production unverified |
| Frontend `src/features` | Auth, user learning screens, admin screens | Vue Router, Pinia, service modules | UI present; coverage differs by route |
| `pybkt` | Python BKT HTTP service | `ThreadingHTTPServer`, pyBKT and scientific Python dependencies | Container and client exist; end-to-end behavior unverified |
| Tests | JUnit Jupiter service/entity tests under backend `src/test/java` | Spring Boot Test | Limited set; no frontend test runner configured |

## Main dependency paths

- Attempt submission persists answers and score, then dispatches adaptive profile update using `@Async("adaptiveTaskExecutor")`; it does not prove analytics or planner updates happen automatically.
- Analytics reads attempt answers and topic/question metadata, then stores gap-analysis and recommendation records.
- Adaptive profile uses the learner's completed objective attempts plus cohort interactions and optionally overlays BKT mastery; heuristic updates remain available.
- Billing webhook validation updates the payment and user plan/entitlement in a transaction.
- Frontend `learningService.js`, `adminService.js`, `userService.js`, and `paymentService.js` wrap Axios calls. A frontend call alone does not prove an equivalent protected backend route exists; compare against `03-api-catalog.md` when changing contracts.

No message broker was identified. Async work uses Spring `@Async`; periodic expiry uses Spring scheduling.
