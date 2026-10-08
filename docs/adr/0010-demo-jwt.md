# ADR 0010 — Demonstration JWT (not a corporate IdP)

- Status: Accepted
- Date: 2026-10-08

## Context

Sprint 5 needs identity so write endpoints and customer-owned orders can be protected. CommerceHub is a
public portfolio project: it must not pretend to be an enterprise identity platform.

## Decision

- The User Service issues signed JWTs with SmallRye JWT Build after bcrypt password verification.
- `sub` and `upn` are the `user_id`. Roles go in the standard `groups` claim (`CUSTOMER` or `ADMIN`).
- Every service verifies the same issuer and the same RSA public key (`jwt/publicKey.pem` on the classpath).
- The matching private key lives only in the User Service (`jwt/privateKey.pem`). The keys in this
  repository are **demonstration material**, not a production secret. Rotate them before any real deployment.
- A bootstrap ADMIN is created on User Service startup when `DEMO_ADMIN_EMAIL` and `DEMO_ADMIN_PASSWORD`
  are set. Seed and catalog writes go through that account. Passwords are never written to the database in
  plain text and must not appear in logs or the README.
- Catalog **reads** stay public. Catalog **writes**, inventory admin, user creation and all order endpoints
  require a Bearer token. Orders are scoped to the JWT subject; another customer receives 403 `order-not-owned`.

## Consequences

- Clients log in at `POST /api/v1/auth/login` and send `Authorization: Bearer`.
- This is not SSO, OIDC, refresh tokens, MFA, key rotation, or a replacement for Keycloak / an IdP.
- Breaking change: `customerId` is no longer accepted on `POST /api/v1/orders` (ADR 0008).
