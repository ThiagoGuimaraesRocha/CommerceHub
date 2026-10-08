# Product Service

Product catalog of CommerceHub: SKU, name, category, price and active flag. Owns `PRODUCT_SCHEMA`.

- OpenAPI: `http://localhost:8082/q/openapi`
- Swagger UI: `http://localhost:8082/q/swagger-ui`
- Health: `http://localhost:8082/q/health`

## Endpoints

| Method | Path | Auth | Description | Success | Errors |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/v1/products` | `ADMIN` | Create a product | 201 + `Location` | 400, 401, 403, 409 `duplicate-sku` |
| `GET` | `/api/v1/products` | public | List (filters `category`, `active`; `page`, `size`) | 200 | 400 |
| `GET` | `/api/v1/products/{id}` | public | Get by id | 200 | 404 |
| `PUT` | `/api/v1/products/{id}` | `ADMIN` | Full update with optimistic locking | 200 | 400, 401, 403, 404, 409 |
| `DELETE` | `/api/v1/products/{id}` | `ADMIN` | Soft delete (`active=false`) | 204 | 401, 403, 404 |

### Rules

- `sku`: unique, uppercase letters, digits and inner hyphens, max 64.
- `categoryCode`: uppercase code (letters, digits, `_`), max 60.
- `price`: `>= 0`, at most 4 decimal places; always returned with scale 4.
- `currencyCode`: ISO 4217, default `BRL`.
- `PUT` must send the `version` returned by the last read.

## Examples

Create:

Writes need an `ADMIN` Bearer token from the User Service. Catalog GET stays public.

```bash
curl -i -X POST http://localhost:8082/api/v1/products \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"sku":"KB-MECH-001","name":"Mechanical Keyboard","categoryCode":"PERIPHERALS","price":349.9}'
```

```http
HTTP/1.1 201 Created
Location: http://localhost:8082/api/v1/products/17c6cbc4-1865-41bc-b0e7-a99d9d49f572
Content-Type: application/json

{"id":"17c6cbc4-1865-41bc-b0e7-a99d9d49f572","sku":"KB-MECH-001","name":"Mechanical Keyboard",
 "description":null,"categoryCode":"PERIPHERALS","price":349.9000,"currencyCode":"BRL","active":true,
 "version":0,"createdAt":"2026-09-29T21:00:55.950624Z","updatedAt":"2026-09-29T21:00:55.950730Z"}
```

List a category:

```bash
curl 'http://localhost:8082/api/v1/products?category=PERIPHERALS&page=0&size=20'
```

Update (send the current `version`):

```bash
curl -X PUT http://localhost:8082/api/v1/products/<id> \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"sku":"KB-MECH-001","name":"Mechanical Keyboard","categoryCode":"PERIPHERALS","price":329.9,"currencyCode":"BRL","active":true,"version":0}'
```

Duplicate SKU:

```json
{
  "type": "urn:commercehub:problem:duplicate-sku",
  "title": "Conflict",
  "status": 409,
  "detail": "A product with SKU KB-MECH-001 already exists",
  "instance": "/api/v1/products"
}
```

Validation error:

```json
{
  "type": "urn:commercehub:problem:validation-error",
  "title": "Bad Request",
  "status": 400,
  "detail": "The request contains invalid fields",
  "instance": "/api/v1/products",
  "violations": [
    { "field": "price", "message": "must be greater than or equal to 0" },
    { "field": "sku", "message": "must contain only uppercase letters, digits and inner hyphens" }
  ]
}
```

## Persistence

- Table `PRODUCTS`, created by Flyway migration `V1__create_products.sql`.
- `version_no` is the JPA `@Version`; `created_at`/`updated_at` use `@CreationTimestamp`/`@UpdateTimestamp`.

## Tests

```bash
./mvnw -pl services/product-service -am verify
```

| Class | Level |
| --- | --- |
| `unit/ProductServiceTest` | Business rules with Mockito |
| `api/ProductResourceTest` | HTTP contract with REST Assured on Oracle |
| `api/SecurityIT` | Public GET; 401/403 on writes |
| `api/HealthEndpointTest` | Health and OpenAPI |
| `integration/ProductRepositoryIT` | Oracle mapping, unique SKU, optimistic locking, timestamps |

Oracle is started automatically by Quarkus Dev Services (Docker required).
