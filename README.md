# Bank Transaction Simulator (Spring Boot + PostgreSQL)

A secure, reliable, high-integrity backend enterprise banking transaction simulation built with Spring Boot, PostgreSQL, Spring Security, and JPA. Designed specifically to demonstrate enterprise financial backend principles including ACID compliance, concurrency control, idempotency, double-entry ledger bookkeeping, audit logging, rate limiting, and security hardening for technical interview discussions.

---

## 1. Project Purpose
The Bank Transaction Simulator models real-world core banking transfer mechanisms, administrative controls, financial ledgers, and transaction security. It provides a non-real-money environment to demonstrate transactional guarantees, race condition mitigation, and security best practices.

---

## 2. Architecture
The project follows a clean layered enterprise architecture:

```
[ Clients / Postman / Tests ]
              │ (HTTPS / REST)
              ▼
    ┌───────────────────┐
    │  Spring Security  │ (JWT Filter, RateLimitFilter)
    └─────────┬─────────┘
              ▼
    ┌───────────────────┐
    │  REST Controllers │ (Client, Admin, Auth)
    └─────────┬─────────┘
              ▼
    ┌───────────────────┐
    │   Service Layer   │ (Transfer, Idempotency, Ledger, Audit, BruteForce)
    └─────────┬─────────┘
              ▼
    ┌───────────────────┐
    │  Data Repositories│ (JPA Repositories, Pessimistic Locking)
    └─────────┬─────────┘
              ▼
    ┌───────────────────┐
    │ PostgreSQL Database│ (Enforced FKs, Unique Constraints, Check Constraints)
    └───────────────────┘
```

---

## 3. Technologies Used
- **Language & Runtime:** Java 21 / OpenJDK 25
- **Framework:** Spring Boot 3.4.3
- **Security:** Spring Security 6.4.3, JJWT (io.jsonwebtoken 0.12.6), BCrypt
- **Persistence:** Spring Data JPA / Hibernate 6.6.8
- **Database:** PostgreSQL 17
- **Validation:** Jakarta Bean Validation
- **Testing:** JUnit 5, MockMvc, Spring Boot Test

---

## 4. Authentication Flow
1. Client sends POST `/auth/login` with `username` and `password`.
2. System checks `BruteForceProtectionService`: if 5 consecutive failures occur within 15 minutes, user is locked.
3. Credentials are verified using BCrypt via Spring Security `AuthenticationManager`.
4. On success, a signed JWT token containing subject (`username`) and role (`BANK_ADMIN` or `CLIENT`) is generated with a 24-hour expiration.
5. On failure, generic `"Invalid credentials"` error is returned, preventing username enumeration.

---

## 5. Authorization Flow
- Stateless authorization via `JwtAuthenticationFilter` on every request.
- Role-based endpoints (`hasRole('BANK_ADMIN')`, `hasRole('CLIENT')`).
- Server-side ownership verification: Clients cannot specify `senderAccountId` or access another user's balance/transactions; the authenticated user is extracted directly from the JWT `SecurityContextHolder`.

---

## 6. Client / Account Creation Flow
1. Admin posts to POST `/admin/clients` with `username` and `password`.
2. Client `User` entity is created with `Role.CLIENT` and status `ACTIVE`.
3. Unique 12-digit account number is generated via `AccountNumberGeneratorService`.
4. `BankAccount` entity is created with `0.00` balance and `ACTIVE` status.
5. Audit log events (`CLIENT_CREATED`, `ACCOUNT_CREATED`) are persisted.

---

## 7. Money Transfer Flow
```
Client POST /client/transfers + Idempotency-Key
       │
       ▼
Check Idempotency Cache (Return cached response if existing)
       │
       ▼
Reserve Idempotency Key in Database
       │
       ▼
Execute Transaction (Deterministic Order Lock min(id) -> max(id))
       │
       ├─► Validate Sender & Receiver Status (ACTIVE)
       ├─► Check Balance sufficiency (amount > 0, sender.balance >= amount)
       ├─► Execute Debit & Credit
       ├─► Create Transaction Record (SUCCESS)
       ├─► Create Double-Entry Ledger (DEBIT + CREDIT)
       └─► Audit Log Success Event
       │
       ▼
Record Response in Idempotency Cache & Return HTTP 201 Created
```

---

## 8. ACID and Rollback
- Money transfers are annotated with `@Transactional`.
- If an exception occurs (e.g., receiver credit failure or unexpected runtime error), Spring's transaction manager triggers a full database rollback.
- Debit, credit, transaction record creation, ledger entries, and audit logs succeed atomically or fail completely together.

---

## 9. Concurrency Problem (Race Conditions)
Without lock isolation, simultaneous transfer requests from Account A (₹1,000) for ₹800 each might both read balance ₹1,000 concurrently, resulting in a illegal negative balance (₹-600).

---

## 10. Pessimistic Locking
- Uses JPA `@Lock(LockModeType.PESSIMISTIC_WRITE)` on `findByIdWithLock`.
- PostgreSQL executes `SELECT ... FOR UPDATE` (or `FOR NO KEY UPDATE`), blocking concurrent transactions until the lock holder commits.
- **Deterministic Lock Ordering:** Accounts are locked in deterministic order (`Math.min(idA, idB)` first, then `Math.max(idA, idB)`), eliminating circular wait conditions and preventing deadlocks.
- `entityManager.refresh(...)` ensures the latest committed balance is reloaded from PostgreSQL under the acquired lock.

---

## 11. Idempotency
- Requests accept an `Idempotency-Key` header.
- Unique key constraint in the `idempotency_keys` database table prevents concurrent duplicate processing.
- Completed responses are serialized and cached in PostgreSQL, allowing retried client requests to receive the identical response without duplicate money movement.

---

## 12. Ledger (Double-Entry Bookkeeping)
- Every successful transfer creates two immutable `LedgerEntry` records:
  - Account A: `DEBIT` ₹X
  - Account B: `CREDIT` ₹X
- System-wide constraint: `Total DEBIT == Total CREDIT` at all times.

---

## 13. Audit Logging
- System events (`LOGIN_SUCCESS`, `LOGIN_FAILURE`, `TRANSFER_SUCCESS`, `TRANSFER_FAILED`, `ACCOUNT_BLOCKED`, etc.) are captured in `audit_logs`.
- Sensitive information (passwords, JWT secrets, plain tokens) is strictly excluded.

---

## 14. Failure Handling
- **Permanent Failures:** Business rule violations (insufficient balance, blocked account, invalid account number) fail immediately without retry.
- **Transient Failures:** Deadlocks or lock acquisition timeouts trigger a controlled retry loop (up to 3 attempts with exponential backoff).

---

## 15. Retry Mechanism
- Controlled retry loop up to 3 attempts.
- Protected by idempotency reservation so retries never cause duplicate money movement.

---

## 16. Rate Limiting
- `RateLimitFilter` uses a sliding-window counter (100 requests/minute for transfer/login).
- Exceeding limit returns `HTTP 429 TOO MANY REQUESTS`.

---

## 17. Brute-Force Login Protection
- `BruteForceProtectionService` locks user accounts after 5 failed login attempts for 15 minutes.
- Successful login resets the failure counter.

---

## 18. Security Decisions
- Passwords hashed with BCrypt.
- Stateless JWT authentication; CSRF disabled safely for REST API.
- OWASP security headers (`X-Content-Type-Options`, `X-Frame-Options: DENY`, `X-XSS-Protection`) enabled.
- Stack traces swallowed in production exception handler (`GlobalExceptionHandler`).

---

## 19. Database Design & Tables
- `users`: User identity, password hash, role (`BANK_ADMIN`/`CLIENT`), status (`ACTIVE`/`BLOCKED`/`INACTIVE`).
- `bank_accounts`: Unique 12-digit account numbers, `BigDecimal` balance (numeric 19,2), status.
- `transactions`: Transfer/deposit/withdrawal reference, sender/receiver FKs, amount, status (`PENDING`/`SUCCESS`/`FAILED`).
- `ledger_entries`: Foreign key to transaction & account, entry type (`DEBIT`/`CREDIT`), amount.
- `idempotency_keys`: Unique key string, user FK, transaction reference, response payload cache.
- `audit_logs`: Username, action, entity type, entity ID, description, result, timestamp.

---

## 20. Testing Strategy
37 automated integration tests in JUnit 5 & Spring Boot Test:
- Authentication & Authorization
- Client & Account Management
- Money Transfer Validation & Balances
- ACID Rollback Verification
- Concurrency & Pessimistic Locking
- Database Idempotency
- Double-Entry Ledger & Audit Logging
- Security Hardening (Rate Limiting, Brute Force, Ownership Protection)

---
