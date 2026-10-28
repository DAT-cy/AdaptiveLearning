# Integrations and Background Work

| Integration/work | Evidence and data path | Failure/retry evidence | Status |
|---|---|---|---|
| MySQL | JPA repositories plus MyBatis `UserMapper`; local profile and Compose configure DB | No shared DB retry policy established | Configured; live connection unverified |
| AI provider | `AiProvider` abstraction; `OpenAiProvider` routes Gemini-compatible/native requests using prompts and text result; `AiServiceImpl` persists evaluations/jobs | Sync writing grade propagates provider errors; job path records errors and state and exposes retry operation | Code present; credentials/provider behavior not tested |
| BKT | `BktClient` sends estimate request (learner/cohort interactions) to `${BKT_URL}`; `pybkt/app.py` uses Python `ThreadingHTTPServer` and exposes health/estimate handlers | Adaptive service treats absent/unready result as heuristic; async dispatcher catches/logs exceptions, with no durable retry | Code present; calibration and live end-to-end unverified |
| Stripe/PayPal | `PaymentStrategy` implementations create checkout and verify/normalize webhook/return events; `PaymentService` validates ID and paid amount/currency | Duplicate event guard by provider+event ID; provider errors mark checkout failed; retries are provider/resend dependent | Unit tests present; real provider validation unverified |
| Google identity | `GoogleTokenVerifier` validates Google ID token, then backend issues own JWT | Failure surfaced by application exception; retry policy not established | Code present; client credentials required |
| SMTP | Email verification service issues/sends codes using Spring mail configuration | Delivery retries/throttling not established from inspected methods | Configurable; actual deliverability unverified |
| PDF/text | RAG service and `PdfTextExtractor` process text/PDF; multipart limits in local config | Unit test for extractor; storage/large-file operational behavior unverified | Partial |
| Frontend HTTP | Axios instance uses `VITE_API_KEY`, timeout and request/response interceptors; JWT added via auth utility | Interceptor handles auth/error status; no backend simulation evidence | Code present |

## Async and scheduled processing

- `AdaptiveUpdateDispatcher.updateAfterAttempt` uses `@Async("adaptiveTaskExecutor")`; the caller returns before profile update finishes. Exceptions are logged and swallowed. There is no durable queue, replay record, or retry demonstrated here.
- `AiServiceImpl.processPendingJobs` is async and processes pending grading jobs with persisted state, idempotency key, retry count/max retries, and output references. A durable scheduler/automatic poll trigger for this method was not confirmed in the inspected controller/service evidence; verify wiring before assuming pending jobs run without an explicit trigger.
- `AttemptServiceImpl.expireOverdueAttempts` is `@Scheduled(fixedDelay = 60_000)` and marks overdue in-progress attempts expired. It does not prove score calculation on expiry.
- `AdaptiveLearningApplication` enables async and scheduling globally.

No broker, distributed lock, object storage integration, or vector database was established in the inspected code. RAG's current implementation should be described from `RagServiceImpl` and repositories, not as embedding/vector retrieval unless such a path is added.

For integration tests, use provider sandbox credentials or mocks, assert signature/amount/event dedupe, simulate BKT unavailable/unready responses, and verify that async/scheduled state transitions persist and recover as expected. Never put credentials in docs or client-exposed `VITE_` variables.
