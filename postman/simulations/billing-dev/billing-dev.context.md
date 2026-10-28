# Billing provider simulation

Generated 2026-10-27. Local stand-ins model the Stripe Checkout Session and PayPal Orders/verify-webhook-signature requests used by the billing strategies. Owner workspace mocks were not queried because Postman CLI is not authenticated.

## Purpose

Exercise the backend provider HTTP clients without real payment credentials or charges. Handler contracts are in `postman/mocks/stripe-billing/default.js` and `postman/mocks/paypal-billing/default.js`.

## Execution

1. `postman simulation run postman/simulations/billing-dev/billing-dev.sim.yaml --port 4900 --output ndjson --no-history`
2. Run a disposable backend on port 9091 with `STRIPE_API_BASE_URL=http://host.docker.internal:4900/stripe`, `PAYPAL_API_BASE_URL=http://host.docker.internal:4900/paypal`, and test-only credentials.
3. Authenticate as the seeded local user and call both checkout providers; then submit signed Stripe webhook requests and PayPal return/webhook requests.
4. Poll payment and entitlement endpoints; stop the disposable backend and simulation after the run.

## Assumptions

- **The local handlers return structurally representative provider responses only.** They do not establish real capture, signature verification, settlement, charge/refund behavior or merchant onboarding. (reasoned 2026-10-27, provider adapter contracts and official provider documentation)
- **PayPal mock capture echoes the order's amount and currency.** VND checkout tests can assert the integer amount through order creation and capture; this does not establish live PayPal account eligibility for VND. (observed 2026-10-28, `postman/mocks/paypal-billing/default.js`)
- **No live payment credentials are installed in the Compose environment.** Simulation credentials and the Stripe test signing secret were supplied only to a disposable local backend; no provider settlement occurred. (observed 2026-10-27, local container configuration and simulation logs)

<!-- begin:observations -->
2026-10-28: full Gradle suite ran `VndCheckoutSimulationTest`; Stripe and PayPal mock members both served checkout requests carrying integer VND amounts and the tests passed.
2026-10-28: `VndCheckoutSimulationTest` sent checkout requests through the Java provider adapters; Stripe received lowercase VND and an integer amount, and PayPal received an integer VND order amount. Both mock members served their modeled responses.
2026-10-27: both mock members served actual backend calls: Stripe checkout and signed success/refund events; PayPal OAuth, order creation, return capture, and verified webhook events.
<!-- end:observations -->

## What didn't work

