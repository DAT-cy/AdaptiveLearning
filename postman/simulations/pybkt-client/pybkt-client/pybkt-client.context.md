# Simulation context — pybkt-client

Generated 2026-10-27 from mocks: `postman/mocks/pybkt`.
Local Postman stand-in for the Python BKT HTTP dependency called by Java `BktClient`.

## Purpose

Stands in for `BKT_URL` paths `/health` and `/v1/mastery/estimate`; lets the Java
client exercise response mapping, HTTP error fallback, and request timeout without
changing or disrupting the running pyBKT container. Handler contract is in
`postman/mocks/pybkt/default.js`.

## Execution

```sh
# start the simulation first
postman simulation run postman/simulations/pybkt-client/pybkt-client/pybkt-client.sim.yaml \
  --port 4918 --output ndjson --no-history

# In another shell, run the client integration tests inside the backend container:
docker exec -e BKT_SIM_BASE_URL=http://host.docker.internal:4918 \
  adaptive-learning-backend gradle test --no-daemon \
  --tests adaptivelearning.module.adaptive.service.BktClientSimulationTest

# The test suite verifies ready=true, ready=false, HTTP 503 fallback, and timeout fallback.

# what actually got called
grep '^{' /tmp/pybkt-client-simulation.log \
  | jq -r 'select(.event=="request") | [.routeKey, .method, .path, .statusCode] | @tsv' \
  | sort | uniq -c

# stop
pkill -INT -f "simulation run .*pybkt-client.sim.yaml"
```

This simulation is unauthenticated and local-only. It routes `/v1/...` to member
`v1`; do not point provider credentials at it.

## Assumptions

- **The simulation models the Java client contract, not pyBKT fitting quality.**
  It returns a fixed mastery probability for ready data; only calls to the real
  Python container establish pyBKT import/fitting behavior. (observed 2026-10-27)
- **Mock scenarios use `x-test-response` for HTTP 503 and delayed-response cases.**
  Removing the header gives the request-shape-based ready/not-ready response.
  (observed 2026-10-27)

- **Requests with no matching mock are forwarded to the real host.**
  A passing run may have hit production. Check what got called for destinations
  you didn't expect and treat them as unmocked.

- **Mocks do not hot-reload.**
  Restart the simulation after editing a handler. Restarting the service is not enough.

- **Only HTTP dependencies are simulated.**
  Databases, caches and queues must run as real local instances.

- **Port 4918 must be free and the client must retain the `/` base path.**
  Otherwise startup fails or the client reaches an unmodelled route. (observed
  2026-10-27; AI and BKT simulations also ran concurrently on unique ports)

## Observations

<!-- begin:observations -->
2026-10-27: the Java `BktClient` simulation tests sent estimate requests for ready and not-ready payloads, an HTTP 503, and a delayed response; all four cases passed after setting the normal response timeout to 10 seconds and the timeout case to 100ms.
2026-10-27: reran `BktClientSimulationTest`; `pybkt` served ready, not-ready, HTTP 503 and delayed-response requests, and all four client tests passed.
2026-10-27: full Gradle suite reran with both AI and BKT simulations active; `pybkt` again served successful, not-ready, HTTP 503 and delayed responses.
<!-- end:observations -->
