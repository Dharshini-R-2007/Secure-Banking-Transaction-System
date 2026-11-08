# Bank Transaction Simulator

A secure banking transaction simulation backend built using **Java, Spring Boot, Spring Security, and PostgreSQL**. Implements reliable money transfers with ACID transactions, concurrency control, idempotency, and double-entry bookkeeping.

## Features

- JWT-based authentication and role-based authorization.
- Client and bank account management.
- Money transfers, deposits, and withdrawals.
- ACID transaction management and rollback.
- Pessimistic locking to prevent race conditions.
- Idempotency to prevent duplicate transactions.
- Double-entry ledger bookkeeping.
- Audit logging for transaction traceability.
- Rate limiting and brute-force login protection.
- Controlled retry and exception handling.

## Tech Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.4.3
- **Security:** Spring Security, JWT, BCrypt
- **Database:** PostgreSQL 17
- **ORM:** Spring Data JPA, Hibernate
- **Build Tool:** Maven
- **Testing:** JUnit 5, Spring Boot Test, MockMvc

## Architecture

The application follows a layered architecture:

```text
Client (Postman)
      |
      v
Spring Security (JWT)
      |
      v
REST Controllers
      |
      v
Service Layer
      |
      v
Repository Layer
      |
      v
PostgreSQL
```

## Database Tables

- `users` – User credentials, roles, and status.
- `bank_accounts` – Account details and balances.
- `transactions` – Financial transaction records.
- `ledger_entries` – Debit and credit entries.
- `idempotency_keys` – Duplicate request prevention.
- `audit_logs` – System activity records.

## Getting Started

### Prerequisites

- Java 21
- PostgreSQL 17
- Maven

### 1. Clone the repository

```bash
git clone <YOUR_REPOSITORY_URL>
cd bank-transaction-simulator
```

### 2. Create the database

```sql
CREATE DATABASE bank_simulator_db;
```

### 3. Configure database credentials

Update `src/main/resources/application.yml` with your PostgreSQL connection details.

Keep database passwords and JWT secrets in environment variables rather than committing them to GitHub.

### 4. Run the application

```bash
mvn spring-boot:run
```

The application runs at `http://localhost:8080`.

## Testing

Run the automated integration tests using:

```bash
mvn test
```

The test suite covers:

- Authentication and authorization.
- Client and account management.
- Money transfer operations.
- ACID rollback.
- Concurrency and pessimistic locking.
- Idempotency.
- Ledger and audit logging.
- Security and rate limiting.

## Key Highlights

- Ensures financial data consistency through ACID transactions.
- Prevents concurrent balance update conflicts using pessimistic locking.
- Avoids duplicate money movements through database-backed idempotency.
- Maintains financial records using double-entry bookkeeping.
- Enforces secure access through JWT and role-based authorization.
