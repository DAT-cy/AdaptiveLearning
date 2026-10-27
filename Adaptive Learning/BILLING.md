# Billing and Premium

Premium is a one-time purchase that permanently unlocks Speaking, the AI tutor, and unlimited generated practice exams/evaluations. A full refund returns the account to Free; existing exams and results remain available. Free accounts can generate five exams total across Listening, Reading, and Writing. The count is atomically consumed when a generated exam is committed and is never decremented when an exam is deleted. Failed generation rolls back the counter with its database transaction.

## Configuration

Copy `.env.example` to `.env` at the Compose project root. Set `PREMIUM_PRICE` as a positive decimal and keep `PREMIUM_CURRENCY=USD` for PayPal compatibility. Configure at least one provider before exposing checkout:

- Stripe: `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`; register `https://<public-host>/api/webhooks/stripe` for `checkout.session.completed`, `checkout.session.async_payment_succeeded`, `checkout.session.async_payment_failed`, and `charge.refunded`.
- PayPal: `PAYPAL_CLIENT_ID`, `PAYPAL_CLIENT_SECRET`, `PAYPAL_WEBHOOK_ID`, `PAYPAL_MODE=sandbox` (or `live`); register `https://<public-host>/api/webhooks/paypal` for `PAYMENT.CAPTURE.COMPLETED`, `PAYMENT.CAPTURE.DENIED`, `PAYMENT.CAPTURE.REFUNDED`, and `PAYMENT.CAPTURE.REVERSED`.
- Set `APP_URL` to the public frontend base URL and `API_URL` to the public backend base URL so provider redirects and PayPal return capture reach the application.
- For local provider simulation, set `STRIPE_API_BASE_URL=http://localhost:4900/stripe` and `PAYPAL_API_BASE_URL=http://localhost:4900/paypal`; leave these defaults as official provider hosts outside simulation.

After changing Compose environment variables, recreate the backend container: `docker compose up -d --no-deps backend`.

## Source of truth and idempotency

The client sends only the provider name. Amount/currency are read from server configuration. Premium is granted only after a verified provider event (or a server-to-PayPal order capture response on the PayPal return path) matches the internal payment ID, amount, and currency. Checkout redirects never grant access. Provider events are uniquely recorded by `(provider, event_id)` and provider payment IDs are unique per provider. The endpoint `GET /api/v1/me/entitlements` supplies the plan and feature limits.

JPA `ddl-auto=update` adds the plan, quota, payment, and event tables using the existing project convention. `LegacyQuotaInitializer` counts legacy user-owned exams once for accounts with an untouched zero counter, capped at `FREE_EXAM_LIMIT`.

## Local provider simulation

Run `postman simulation run postman/simulations/billing-dev/billing-dev.sim.yaml --port 4900 --output ndjson`. The local Stripe and PayPal stand-ins cover only the outbound Checkout Session, Orders/capture, OAuth token, and PayPal webhook-verification calls used by the adapters. They do not model provider settlement, Dashboard webhook registration, refunds at the provider, or signature authenticity. Do not use them as production webhook senders.

Stripe account availability depends on the merchant's country; verify it against Stripe's current [global availability](https://stripe.com/global) before going live. Configure real credentials and HTTPS webhook URLs in the provider dashboards before accepting payments.
