# Sprint 5 — User Service + JWT

Goal: demonstration identity and endpoint protection with JWT. The mechanism is for the portfolio
environment only and does not replace a corporate IdP.

## Delivered

- User Service `0.5.0`: Flyway `APP_USERS`, bcrypt password hashing, login that issues a signed JWT
  (`sub`/`upn` = `user_id`, `groups` = role), user create (ADMIN), `/users/me`.
- Bootstrap ADMIN on startup from `DEMO_ADMIN_EMAIL` / `DEMO_ADMIN_PASSWORD` (env, not README).
- Product Service `0.5.0`: catalog GET public; POST/PUT/DELETE require `ADMIN`.
- Order Service `0.5.0`: all order endpoints authenticated; `customerId` comes from the JWT subject
  (removed from `POST /orders` body and from the list query). A customer only reads/confirms/cancels
  their own orders (403 `order-not-owned`).
- Inventory Service `0.5.0`: admin GET/PUT require `ADMIN`.
- Shared demo RSA key pair; public key copied per service (no shared library). Issuer via `JWT_ISSUER`.
- Seed logs in as the bootstrap admin, then creates users/products/stock through the APIs.
- Tests: hasher, user service, JWT claims, API contracts, SecurityIT (401/403) on every service.
- ADR 0010, plan v0.7, Postman Auth folder, service READMEs.

## Definition of Done

| Criterion | Status | Evidence |
| --- | --- | --- |
| Password stored only as hash | Done | `PasswordHasher`, `UserServiceTest` |
| Login issues a signed JWT | Done | `JwtServiceTest`, `AuthResourceTest`, `SecurityIT` |
| Protected endpoints reject missing/invalid token; role tested | Done | `SecurityIT` in user/product/order/inventory |
| Tokens, passwords and hashes never in logs or README | Done | AuthResource logs user id only; README uses env placeholders |
| Docs state the demo security limit | Done | ADR 0010, User Service README, plan v0.7 |

## Versions added

| Item | Value |
| --- | --- |
| User / Product / Order / Inventory images | `ghcr.io/<owner>/commercehub-*-service:0.5.0` |
