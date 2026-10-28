# Completeness and Risks

Ratings are repository evidence, not production-readiness claims. “Verified” means code path exists; runtime operation against external services remains unverified.

| Feature | UI | API | Logic/data | Tests | Assessment |
|---|---|---|---|---|---|
| Login/signup/email verification | Auth screens and service | Auth controller | User + verification persistence, JWT | Email verification and access-state tests | Implemented; mail/live Google unverified |
| Exam and objective attempt | Learning views/service | Exam/attempt controllers | Timed attempt, answer save, scoring, expiry | No attempt service test found | Core path present; end-to-end unverified |
| Billing/entitlements | Pricing/payment views and service | Payment/webhook/entitlement controllers | Strategy, event dedupe, plan/quota changes | Payment and entitlement tests | Implemented with provider runtime unverified |
| Analytics | Progress views/service | Analytics controller | Gap analysis, progress, recommendations | Analytics service tests | Implemented in service; UI/API scenario unverified |
| Adaptive/BKT | Client service usage | Adaptive controller | Heuristic plus optional BKT request | No adaptive test found | Partial; data calibration unverified |
| AI writing grading/jobs | UI/admin service references | AI controller | Sync grade and job processing paths | No AI test found | Partial; full lifecycle/scheduler wiring unverified |
| Content generation | Admin UI/service | Content generation controller | Generation/application code exists | No tests found | Partial; AI/live and review policy verify |
| Tutor | User UI/service | Tutor controller | Conversation/message persistence and provider path | No test found | Partial; citations/limits/runtime unverified |
| RAG/PDF | Admin/user interfaces | RAG/admin knowledge-base routes | Documents/chunks/search and PDF extraction | PDF extractor test | Partial; no vector retrieval established |
| Speaking | Frontend surface limited/unclear | Speaking controller | Submission/evaluation entities/services | No tests found | Partial; do not assume audio/STT or integrated grading |
| Planner | User screen/service | Planner controller | Plans/tasks and summary logic | No tests found | Partial; AI scheduling not established |

## Highest-priority risks

| Severity | Evidence and possible impact | How to verify / direction |
|---|---|---|
| Critical | Local config files appear to contain credential-like literal values (including provider/secret fields), and Compose has development fallbacks. Leaking these files could compromise external accounts or signing credentials. Values are intentionally not reproduced here. | Inspect `application-local.yml` and `docker-compose.yml` privately; rotate any real exposed credentials, move to environment/secret manager, and scan git history. |
| High | `UserServiceImpl.initAdminUser()` seeds `admin/admin` and `user/user` when no admin exists. An exposed deployment could start with known credentials. | Gate seed users to explicit local/dev profile or require generated credentials; verify deployed profiles. |
| High | Adaptive updates use in-process async dispatch and swallow exceptions; process restart or executor failure can leave profiles stale without a replay queue. | Add persistent outbox/job state if updates must be guaranteed; test executor saturation/restart. |
| High | Security allowlists broad patterns and most module endpoints rely on default authenticated access; resource-level ownership/role checks vary by controller. | Audit every endpoint against user/owner/admin policy, especially analytics, AI jobs, contentgen and admin routes; add authorization tests. |
| Medium | AI job processing has persisted retry/idempotency fields, but automatic trigger/scheduler was not confirmed; jobs could remain pending. | Trace all callers of `processPendingJobs`; integration-test PENDING through terminal state. |
| Medium | Exam expiry job only marks `EXPIRED` in located method; unanswered final scoring/analytics behavior is unclear. | Decide intended semantics and test scheduler plus result endpoints. |
| Medium | `ddl-auto: update` and sparse migration evidence can permit schema drift or risky production schema changes. | Use reviewed versioned migrations and test upgrade from representative existing schema. |
| Medium | Attempt submit invokes synchronous scoring; no attempt test was found. Duplicate submits, concurrent answer writes, expiry races and ESSAY behavior need evidence. | Add focused service/integration tests for lifecycle, ownership, question types and concurrency. |
| Low/Medium | Frontend has no test command in `package.json`; services and UI can drift from backend contracts. | Add contract or browser tests, or document manual smoke-test paths. |

The most serious issue is configuration hygiene: documentation should not duplicate any values found in config. This analysis did not run provider calls, deploy containers, or inspect production infrastructure.
