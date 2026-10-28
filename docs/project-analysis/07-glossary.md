# Glossary

| Term | Meaning in this repository |
|---|---|
| Attempt | One learner's timed run of an exam, persisted as `ExamAttempt`. |
| Answer | A learner response stored in `AttemptAnswer`, with JSON value, correctness/points where available, time and order. |
| Certificate | Top-level learning/exam catalog such as a certification target; related by IDs to topics, questions and plans. |
| Topic / skill | Content classification used for question organization, analytics and adaptive ability. Adaptive currently keys heuristic values by `TOPIC_{topicId}`. |
| Gap analysis | Per-attempt JSON summary of accuracy by skill/topic/difficulty and weak/strong areas. |
| Recommendation | Persistent suggested practice/action derived from analytics, with priority and lifecycle status. Separate from adaptive difficulty recommendation. |
| Learner profile | Per-user/certificate ability JSON and attempt count used by adaptive recommendation/forecast. |
| BKT | Bayesian Knowledge Tracing service used to estimate mastery from objective response sequences when its estimate is ready; not equivalent to a calibrated IELTS/TOEIC score. |
| Rubric | Criteria and scoring instructions supplied to AI evaluation. |
| AI job | Persisted asynchronous grading task with status, idempotency key, retries and output reference. Its automatic processing trigger needs verification. |
| RAG | Document/chunk retrieval path used to provide context; inspected code establishes text/PDF extraction and search, not vector embeddings. |
| Entitlement | Plan-derived access (FREE/PREMIUM), exam quota, skills and configured payment options returned by billing module. |
| Webhook event | Provider payment notification verified and normalized before mutating payment/entitlement state. Duplicate identity is provider + event ID. |
| `DefaultRes` | Common backend response wrapper containing status/message/data fields. |
| `CHƯA XÁC MINH` | Repository evidence is incomplete for a runtime, integration, authorization, or deployment claim. |

## Source of truth

For behavior, prefer controller/service/entity implementation and tests over illustrative scenarios in `Adaptive Learning/BUSINESS.md`. `Adaptive Learning/BILLING.md` documents billing rules but provider setup/production status still requires live configuration and sandbox verification. This guide is an initial repository analysis, not an exhaustive audit of every DTO or route.
