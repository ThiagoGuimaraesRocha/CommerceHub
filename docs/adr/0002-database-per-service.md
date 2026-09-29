# ADR 0002 — Database per service using one Oracle schema per service

- Status: Accepted
- Date: 2026-09-29

## Context

Services must be independently deployable and must not couple through shared tables. Running one Oracle
instance per service locally would be too heavy for a developer machine and for a free OpenShift sandbox.

## Decision

- A single Oracle Database Free instance, pluggable database `FREEPDB1`.
- One schema (database user) per service: `USER_SCHEMA`, `PRODUCT_SCHEMA`, `ORDER_SCHEMA`, `INVENTORY_SCHEMA`.
- Each service connects **only** with its own credentials and owns its tables.
- No foreign key crosses schemas. Cross-service data is obtained through APIs or events
  (for example, order items keep a price snapshot instead of referencing the product table).
- Schemas are created on the first container start by `infra/oracle/init/01-create-service-schemas.sh`,
  using the image's `createAppUser` helper. Passwords come from `.env`, never from Git.
- Tables are introduced by each service in its own sprint (starting in Sprint 2).

## Consequences

- Isolation is enforced by database privileges, not only by convention.
- The physical instance is shared, so this is a logical isolation; moving a schema to its own instance later
  only requires changing that service's `DB_URL`.
- Schema changes are versioned per service with migrations (to be introduced with the first tables).
