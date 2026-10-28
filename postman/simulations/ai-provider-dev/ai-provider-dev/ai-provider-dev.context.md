# Simulation context — ai-provider-dev

Generated 2026-10-27 from mocks: `postman/mocks/ai-provider`.
This file is how the next session starts without redoing the setup. Fill in each
@todo as you work, and update it when something you relied on turns out wrong.

## Purpose

Stands in for the OpenAI-compatible chat-completions endpoint used by
`OpenAiProvider`; it allows client parsing and provider-error handling tests
without real AI credentials. Handler contract is in
`postman/mocks/ai-provider/default.js`.

## Execution

```sh
# start the simulation first
postman simulation run postman/simulations/ai-provider-dev/ai-provider-dev/ai-provider-dev.sim.yaml \
  --port 4920 --output ndjson --no-history > /tmp/ai-provider-dev-simulation.log 2>&1 &

# In another shell, run the client integration tests inside the backend container:
docker exec -e AI_SIM_BASE_URL=http://host.docker.internal:4920/ai/v1/ \
  adaptive-learning-backend gradle test --no-daemon \
  --tests adaptivelearning.module.ai.gateway.OpenAiProviderSimulationTest

# The tests verify a valid completion response and sanitized provider-error handling.

# what actually got called
grep '^{' /tmp/ai-provider-dev-simulation.log \
  | jq -r 'select(.event=="request") | [.routeKey, .method, .path, .statusCode] | @tsv' \
  | sort | uniq -c

# stop
pkill -INT -f "simulation run .*ai-provider-dev.sim.yaml"
```

The simulation is local-only and unauthenticated; the test supplies a
test-only bearer value to the mock. No real provider key is needed.

## Assumptions

- **The mock models only `POST /v1/chat/completions` with fixed success/error
  responses.** A pass proves the Java HTTP client path, not model quality,
  quotas, provider authentication or live-service availability. (observed
  2026-10-27)

- **Requests with no matching mock are forwarded to the real host.**
  A passing run may have hit production. Check what got called for destinations
  you didn't expect and treat them as unmocked.

- **Mocks do not hot-reload.**
  Restart the simulation after editing a handler. Restarting the service is not enough.

- **Only HTTP dependencies are simulated.**
  Databases, caches and queues must run as real local instances.

- **Port 4920 must be free and the client must retain the `/ai/v1/` path.**
  Otherwise startup fails or the client reaches an unmodelled route. (observed
  2026-10-27; AI and BKT simulations also ran concurrently on unique ports)

## Observations

<!-- begin:observations -->
2026-10-27: `ai-provider` served the client's provider-error 400 and successful 200 requests; both simulation tests passed.
2026-10-27: full Gradle suite reran with both AI and BKT simulations active; `ai-provider` again served 400 and 200 responses.
<!-- end:observations -->

## What didn't work
