# Demonstration JWT keys

RSA key pair used by CommerceHub to **sign** (User Service) and **verify** (every service) demo JWTs.

These files are portfolio material, not production secrets. See [ADR 0010](../../docs/adr/0010-demo-jwt.md).

Classpath copies (raw PEM, no comments):

- `services/user-service/src/main/resources/jwt/privateKey.pem` — signing
- `services/*/src/main/resources/jwt/publicKey.pem` — verification
