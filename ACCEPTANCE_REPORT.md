# Adaptive Learning Acceptance Regression Report

Date: 2026-10-27  
Baseline: `c421dc7f986145debec80b49d08468d73a04d2fd` (`HEAD`; not changed)

## Status Summary

| Status | Meaning |
|---|---|
| PASS | The described test ran and passed; scope is limited to the evidence listed. |
| FAIL | A reproducible check ran and failed. |
| BLOCKED | The check could not run because required runtime configuration, credentials, or access was unavailable. |
| NOT TESTED | No test was run for this behavior. |

Backend full suite: **PASS**, 28 test classes / 117 tests / 0 skipped / 0 failures / 0 errors. Frontend production build: **PASS**. The full backend suite ran with the local AI and BKT HTTP simulations enabled; those results do not establish behavior of the real providers.

## Regression Matrix

| Function / test case | Result | Actual evidence, limitation, and reproduction |
|---|---|---|
| Backend build and tests | PASS | `docker exec -e BKT_SIM_BASE_URL=http://host.docker.internal:4918 -e AI_SIM_BASE_URL=http://host.docker.internal:4920/ai/v1/ adaptive-learning-backend gradle test --rerun-tasks --no-daemon` exited 0. The report contained 28 suites and 117 tests with no skip/fail/error. |
| Frontend production build | PASS | `npm run build` in `AdaptiveLearningFE/` exited 0; Vite transformed 2,285 modules and emitted the production bundle. There is no frontend test script, so this is not UI interaction coverage. |
| Compose configuration | BLOCKED | `docker compose -f "Adaptive Learning/docker-compose.yml" config --quiet` exited 1 because required `MYSQL_ROOT_PASSWORD` and `JWT_SECRET` were absent from this shell's environment. No placeholders or secrets were supplied. Provide local values through the documented ignored `.env`, then rerun the command. |
| Signup, duplicate signup, email verification, resend, password login | PASS (service tests) / BLOCKED (E2E) | `UserServiceImplTest`, `EmailVerificationServiceTest` and `UserBootstrapSecurityTest` passed in the full suite. They cover duplicate/unverified users, bad/expired codes, cooldown/rate-limit and login failures using mocks. No real signup→email→verify→login request sequence ran; real SMTP delivery is unverified and no authenticated test account was supplied. Reproduce E2E only with a disposable account and test SMTP. |
| Profile endpoint after login | NOT TESTED | Unit tests create/validate login data but do not execute the complete token→`/api/v1/auth/profile` HTTP flow. The running backend's `/actuator/health` returned HTTP 401, so unauthenticated runtime access was not a valid profile check. |
| Catalog → start attempt → save/restore answer → submit → result/history | PASS (service tests) / BLOCKED (E2E) | `AttemptServiceImplTest`, `AttemptOwnershipTest` and `SecurityAuthorizationIntegrationTest` passed. Catalog access was tested with `MockMvc` and a mocked role; the authenticated cross-request user journey with persisted catalog/attempt data was not run. Runtime API needs a valid JWT and seeded test data. |
| Analytics and recommendations | PASS (service tests) | `AnalyticsAnalysisTest`, `AnalyticsOwnershipTest`, `AnalyticsServiceImplTest` and `AdaptiveServiceImplTest` passed for analysis/recommendation/adaptive service cases. No authenticated API chain from submitted real attempt to updated analytics was run. |
| BKT client error/fallback/timeout | PASS (simulation) | Full suite ran `BktClientSimulationTest` against the local Postman mock. The `pybkt` member received ready, not-ready, HTTP 503 and delayed requests; tests passed. This proves Java client mapping/fallback against the stand-in only. |
| Real BKT health | PASS (health only) / NOT TESTED (mastery estimate) | `adaptive-learning-pybkt` reported healthy; direct `/health` returned HTTP 200. No real model-estimate request or model calibration/forecast quality check was performed. |
| Planner create/read/task update/summary/complete/archive and ownership | PASS (service tests) / NOT TESTED (UI E2E) | `PlannerLifecycleTest` and `PlannerOwnershipTest` passed in the suite. Vue routes compiled, but no browser session drove plan creation/task changes against the API. |
| AI grading/rubric, ownership and validation | PASS (service tests) | `AiGradingServiceTest` and `AiOwnershipTest` passed with mocked providers/data. This does not establish real AI grading quality. |
| AI provider HTTP success and provider error | PASS (simulation) | Full suite ran `OpenAiProviderSimulationTest`; mock received `POST /v1/chat/completions` with 400 and 200 responses. No live AI credentials/provider request was used. AI provider timeout against a delayed HTTP response was NOT TESTED in this run. |
| AI jobs enqueue, idempotency, retry and result | PASS (service tests) | `AiJobLifecycleTest` and `AiJobSchedulerTest` passed for enqueue/no-inline-provider, duplicate reuse, compare-and-set claim, retry/max retries and sanitized provider failure. Scheduler delegation is invoked directly in tests; an actual scheduled poll against persisted jobs was NOT TESTED. Multi-node database race and recovery of a job stranded in `PROCESSING` after process death are NOT TESTED. |
| Tutor conversation/message | PASS (service tests) | `TutorServiceImplTest` passed with mocked provider and owner checks. Real provider/UI conversation flow is NOT TESTED. |
| RAG upload/extraction/search/ownership | PASS (service tests) | `RagServiceImplTest` and `PdfTextExtractorTest` passed. Search is the current database/token implementation; these tests do not establish vector embeddings or semantic vector search. |
| Content generation/apply | PASS (service tests) | `ContentGenerationServiceImplTest` passed with mocked provider and source validation. Real generation/provider latency and authenticated admin review/apply flow are NOT TESTED. |
| Speaking submission/evaluation | PASS (service tests) | `SpeakingServiceImplTest` passed with mocked provider and ownership checks. No audio upload, speech-to-text or real speech provider was exercised. |
| Checkout, pending payment, checkout error | NOT TESTED (provider integration) | `PaymentServiceTest` and `EntitlementServiceTest` passed, but the current regression run did not call Stripe or PayPal checkout adapters. No real charge was created. The checkout/PENDING/checkout-error sandbox paths remain unverified in this run. |
| Webhook signature, malformed payload, event duplicate, payment identity/amount/currency and entitlement | PASS (limited service cases) / NOT TESTED (provider verification) | `PaymentServiceTest` verifies a duplicate already-normalized success event does not grant twice, amount mismatch rejection and refund revocation. It does not exercise raw Stripe/PayPal signatures, malformed provider payloads, wrong identity/currency, DB transaction rollback or a provider webhook end-to-end. Provider sandbox was not contacted. |
| Authorization roles and ownership | PASS (test-level) / NOT TESTED (all live APIs) | `SecurityAuthorizationIntegrationTest` passed anonymous/admin/user checks and CORS cases; module tests cover selected foreign-owner cases for attempts, AI, analytics, planner, tutor, RAG and speaking. This is not exhaustive endpoint coverage, nor a live JWT-backed cross-user request matrix. |
| JWT expiry | PASS (token unit test) / NOT TESTED (HTTP) | `JwtServiceTest` passed the expired-claim test. No expired bearer token was sent through the running HTTP filter and frontend redirect behavior was not browser-tested. |
| SMTP error and timeout | PASS (service-level configuration case) / BLOCKED (real SMTP) | `EmailVerificationServiceTest` covers mail-not-configured behavior with a mock. No SMTP connection, delivery, provider error or network timeout was exercised. |
| Duplicate request/event and missing data | PASS (selected service cases) / NOT TESTED (concurrent/runtime) | AI duplicate job and payment duplicate normalized event tests passed; service tests cover selected missing entity/invalid inputs. Concurrent duplicate DB inserts, webhook replay across application nodes, and scheduled-worker crash recovery were not tested. |
| Runtime ports/dependencies | PASS (availability only) | During audit ports 9090, 3000 and 3306 accepted connections; MySQL and pyBKT containers reported healthy. The backend actuator endpoint returned 401. Port reachability/health does not prove authenticated product flows. |

## Findings and Remediation

| Priority | Finding / severity | Reproduction / evidence | Recommended action |
|---|---|---|---|
| P0 | `docker-compose.yml` contains non-empty secret fallbacks for provider/mail credentials. Treat these values as exposed; this report intentionally omits them. | Inspect only variable declarations at `Adaptive Learning/docker-compose.yml` lines 60, 66, 76 and 77; do not print or copy their values. | Remove credential defaults from Compose, require ignored local `.env`/deployment secret injection, rotate affected credentials with their providers, and scan commit history/CI logs for exposure. |
| P1 | Current workspace cannot validate Compose from a clean shell because required DB/JWT variables are absent. | The `docker compose ... config --quiet` command above exits 1 and names missing required variables. | Add a local ignored `.env` from the safe example, populate values outside source control and rerun `config --quiet` plus `up`/health checks. |
| P1 | No complete authenticated API regression journey exists for signup/profile, exam attempts, analytics, Planner UI or the cross-module ownership matrix. | Full backend suite uses unit tests and `MockMvc`; no frontend test script or authenticated E2E run was available. | Add a disposable integration profile with isolated DB/mail/provider mocks and reproducible HTTP/browser tests. Keep tests from using production data or credentials. |
| P1 | Billing raw-webhook verification and sandbox checkout flow lack current-run evidence for malformed signatures/payloads, wrong provider identity/currency and transaction rollback. | The payment test suite currently verifies only normalized duplicate success, amount mismatch and refund behavior; no provider adapter sandbox call was run. | Add strategy/controller tests for each provider's raw signature and payload contracts, transactional rollback/duplicate persistence, then run sandbox integration before release. |
| P1 | AI jobs have service retry/CAS coverage but no proof of scheduler execution against persisted jobs or recovery after a worker dies in `PROCESSING`. | Current scheduler test calls the scheduled method directly; no persisted asynchronous poll test ran. | Add a scheduler integration test and a bounded stale-job recovery/lease policy with concurrency tests. |
| P2 | SMTP and real AI provider behavior, including real latency/timeout and mail delivery, remain unverified. | AI/BKT local simulations passed; mock evidence does not represent provider availability, quotas or live auth. | Run controlled staging tests with provider sandbox/test credentials, bounded inputs and no production-recipient delivery. |
| P2 | No browser-level regression suite validates loading/empty/error states, 401/403 routing, JWT refresh/expiry, navigation or task/payment UI interactions. | `AdaptiveLearningFE/package.json` defines build/dev/preview only; `npm run build` passed. | Add end-to-end tests only after choosing/approving a test runner; meanwhile preserve API contract and manual QA evidence per route. |
| P3 | Forecast calibration, exam content quality, vector retrieval, audio/STT and live AI evaluation quality are not established by current implementation tests. | Relevant tests use deterministic mocks or current text/database implementations. | Treat these as product validation/development work; define acceptance data and metrics before claiming calibrated forecasts, vector search or audio evaluation. |

## Commands Run

```text
docker exec -e BKT_SIM_BASE_URL=http://host.docker.internal:4918 -e AI_SIM_BASE_URL=http://host.docker.internal:4920/ai/v1/ adaptive-learning-backend gradle test --rerun-tasks --no-daemon
npm run build
docker compose -f "Adaptive Learning/docker-compose.yml" config --quiet
docker exec adaptive-learning-pybkt python -c "import urllib.request; r=urllib.request.urlopen('http://localhost:8001/health',timeout=3); print('pybkt_health_http='+str(r.status))"
curl.exe -sS -o NUL -w "backend_actuator_http=%{http_code}\n" http://localhost:9090/actuator/health
```

The AI and BKT simulations were local Postman stand-ins. No live AI call, SMTP delivery, Stripe/PayPal transaction, production webhook, secret value or payment settlement was used in this regression run.
