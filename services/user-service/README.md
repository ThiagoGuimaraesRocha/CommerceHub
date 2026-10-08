# User Service

Demo identity of CommerceHub: user directory and JWT issuance. Owns `USER_SCHEMA`.

This is a **portfolio authentication mechanism**, not a corporate IdP. There is no SSO, OIDC, refresh
token, MFA or key rotation. See [ADR 0010](../../docs/adr/0010-demo-jwt.md).

- OpenAPI: `http://localhost:8081/q/openapi`
- Swagger UI: `http://localhost:8081/q/swagger-ui`
- Health: `http://localhost:8081/q/health`

## Endpoints

| Method | Path | Auth | Description | Success | Errors |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/v1/auth/login` | public | Issue a Bearer JWT | 200 | 400, 401 `invalid-credentials` |
| `POST` | `/api/v1/users` | `ADMIN` | Create a user | 201 + `Location` | 400, 401, 403, 409 `duplicate-email` |
| `GET` | `/api/v1/users/me` | authenticated | Current user | 200 | 401 |
| `GET` | `/api/v1/users/{id}` | `ADMIN` | Get by id | 200 | 401, 403, 404 |

### Rules

- Passwords are stored only as bcrypt hashes. They never appear in logs.
- Login always returns the same 401 message for unknown, inactive or wrong-password accounts.
- JWT `sub` and `upn` are the `user_id`. Roles are in `groups` (`CUSTOMER` or `ADMIN`).
- A bootstrap ADMIN is created on startup when `DEMO_ADMIN_EMAIL` and `DEMO_ADMIN_PASSWORD` are set.

## Examples

Login (do not paste the token into logs or the README):

```bash
curl -sS -X POST http://localhost:8081/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"<admin-email-from-env>","password":"<admin-password-from-env>"}'
```

Create a customer (ADMIN token):

```bash
curl -i -X POST http://localhost:8081/api/v1/users \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"email":"customer@commercehub.local","password":"<choose-a-password>","fullName":"Ana Customer","roleCode":"CUSTOMER"}'
```

Invalid credentials:

```json
{
  "type": "urn:commercehub:problem:invalid-credentials",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Invalid email or password",
  "instance": "/api/v1/auth/login"
}
```

## Persistence

- Table `APP_USERS`, Flyway `V1__create_app_users.sql`.
- Unique email (stored lowercased). Role CHECK: `CUSTOMER` or `ADMIN`.

## Configuration

| Variable | Default | Description |
| --- | --- | --- |
| `USER_DB_PASSWORD` | required | Schema password |
| `JWT_ISSUER` | `https://commercehub.example/issuer` | Must match every other service |
| `JWT_LIFESPAN_SECONDS` | `3600` | Access token lifetime |
| `DEMO_ADMIN_EMAIL` / `DEMO_ADMIN_PASSWORD` | empty (skip bootstrap) | Bootstrap ADMIN |

## Tests

```bash
./mvnw -pl services/user-service -am verify
```

| Class | Level |
| --- | --- |
| `unit/PasswordHasherTest` | bcrypt hash/match |
| `unit/UserServiceTest` | create, duplicate email, login rules |
| `unit/JwtServiceTest` | signed token claims, no password in payload |
| `api/AuthResourceTest` | login HTTP contract |
| `api/UserResourceTest` | create / get / 409 / 403 |
| `api/SecurityIT` | 401/403 and real JWT round-trip to `/me` |
| `api/HealthEndpointTest` | Health and OpenAPI |

Oracle is started automatically by Quarkus Dev Services (Docker required).
