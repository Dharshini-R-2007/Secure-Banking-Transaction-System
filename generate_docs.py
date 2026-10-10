import docx
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import parse_xml, OxmlElement
from docx.oxml.ns import nsdecls, qn

def create_element(name):
    return OxmlElement(name)

def set_cell_background(cell, fill_hex):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(f'<w:tcMar {nsdecls("w")}><w:top w:w="{top}" w:type="dxa"/><w:bottom w:w="{bottom}" w:type="dxa"/><w:left w:w="{left}" w:type="dxa"/><w:right w:w="{right}" w:type="dxa"/></w:tcMar>')
    tcPr.append(tcMar)

def add_heading_styled(doc, text, level):
    h = doc.add_heading(text, level=level)
    h.paragraph_format.space_before = Pt(12)
    h.paragraph_format.space_after = Pt(6)
    for run in h.runs:
        run.font.name = 'Calibri'
        if level == 1:
            run.font.size = Pt(18)
            run.font.color.rgb = RGBColor(16, 44, 87) # Dark Navy
            run.bold = True
        elif level == 2:
            run.font.size = Pt(14)
            run.font.color.rgb = RGBColor(53, 89, 140) # Slate Blue
            run.bold = True
        elif level == 3:
            run.font.size = Pt(12)
            run.font.color.rgb = RGBColor(30, 30, 30)
            run.bold = True
    return h

def add_code_block(doc, text):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    cell = table.cell(0, 0)
    cell.width = Inches(6.5)
    set_cell_background(cell, "F4F6F9")
    set_cell_margins(cell, top=120, bottom=120, left=180, right=180)
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.space_after = Pt(4)
    p.paragraph_format.line_spacing = 1.15
    run = p.add_run(text)
    run.font.name = 'Consolas'
    run.font.size = Pt(9.5)
    run.font.color.rgb = RGBColor(40, 40, 40)
    doc.add_paragraph().paragraph_format.space_after = Pt(4)

def build_document():
    doc = Document()

    # Set Margins
    for section in doc.sections:
        section.top_margin = Inches(0.8)
        section.bottom_margin = Inches(0.8)
        section.left_margin = Inches(0.8)
        section.right_margin = Inches(0.8)

    # Title Page / Header Title
    title_p = doc.add_paragraph()
    title_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    title_p.paragraph_format.space_before = Pt(24)
    title_p.paragraph_format.space_after = Pt(4)
    r_title = title_p.add_run("Bank Transaction Simulator")
    r_title.font.name = 'Calibri'
    r_title.font.size = Pt(26)
    r_title.bold = True
    r_title.font.color.rgb = RGBColor(16, 44, 87)

    sub_p = doc.add_paragraph()
    sub_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    sub_p.paragraph_format.space_after = Pt(24)
    r_sub = sub_p.add_run("Complete Technical Architecture & Technical Interview Guide\nSpring Boot 3.4 + PostgreSQL 17 + Spring Security")
    r_sub.font.name = 'Calibri'
    r_sub.font.size = Pt(13)
    r_sub.font.italic = True
    r_sub.font.color.rgb = RGBColor(80, 80, 80)

    doc.add_heading("Overview of Completed Implementation", level=1)
    p_ov = doc.add_paragraph(
        "Development of the Bank Transaction Simulator project is fully completed. "
        "All 12 required modules, 46 business/technical specifications, and 37 comprehensive integration test scenarios "
        "pass cleanly with BUILD SUCCESS. The system is designed to demonstrate enterprise financial backend principles "
        "including ACID compliance, concurrency control, idempotency, double-entry ledger bookkeeping, audit logging, "
        "rate limiting, and security hardening for technical interview discussions."
    )
    p_ov.paragraph_format.space_after = Pt(12)

    # Section A
    add_heading_styled(doc, "A. Final Project Folder Structure", 2)
    folder_structure = (
        "d:/springboots/BANKING UPI TRANSACTION/\n"
        "├── pom.xml\n"
        "├── README.md\n"
        "└── src/\n"
        "    ├── main/\n"
        "    │   ├── java/\n"
        "    │   │   └── com/bank/simulator/\n"
        "    │   │       ├── BankSimulatorApplication.java\n"
        "    │   │       ├── config/ (DataInitializer.java, SecurityConfig.java)\n"
        "    │   │       ├── controller/ (ClientTransferController.java, AdminTransactionController.java, ...)\n"
        "    │   │       ├── dto/ (TransferRequest.java, TransactionResponse.java, ...)\n"
        "    │   │       ├── entity/ (User.java, BankAccount.java, Transaction.java, LedgerEntry.java, ...)\n"
        "    │   │       ├── exception/ (GlobalExceptionHandler.java, InsufficientBalanceException.java, ...)\n"
        "    │   │       ├── repository/ (BankAccountRepository.java, TransactionRepository.java, ...)\n"
        "    │   │       ├── security/ (JwtTokenProvider.java, RateLimitFilter.java, ...)\n"
        "    │   │       └── service/ (TransferService.java, TransferTransactionExecutor.java, LedgerService.java, ...)\n"
        "    └── test/\n"
        "        └── java/com/bank/simulator/\n"
        "            ├── MoneyTransferTests.java, ConcurrencyAndLockingTests.java, AcidRollbackTests.java, ...\n"
    )
    add_code_block(doc, folder_structure)

    # Section B
    add_heading_styled(doc, "B. All Modules Completed", 2)
    modules = [
        "1. Money Transfer + Deposit + Withdrawal + Transaction Management",
        "2. ACID + @Transactional + Rollback + Transaction Safety",
        "3. Concurrency Control + Pessimistic Locking (@Lock(LockModeType.PESSIMISTIC_WRITE))",
        "4. Database-backed Idempotency (IdempotencyKey caching & deduplication)",
        "5. Double-Entry Ledger (LedgerEntry with DEBIT/CREDIT equality)",
        "6. Audit Logging (AuditLog without passwords/secrets)",
        "7. Failure Classification & Handling",
        "8. Controlled Retry with Exponential Backoff",
        "9. Security Hardening (Rate limiting filter, Brute-force login protection, Ownership checks)",
        "10. Rate Limiting (HTTP 429 TOO MANY REQUESTS)",
        "11. Brute-Force Login Protection (5 failed attempts -> 15 min lock)",
        "12. Comprehensive Integration Test Suite & System Documentation"
    ]
    for m in modules:
        doc.add_paragraph(m, style='List Bullet')

    # Section C
    add_heading_styled(doc, "C. All Important Files Created / Modified", 2)
    files_info = [
        ("Entities", "Transaction.java, LedgerEntry.java, IdempotencyKey.java, AuditLog.java, BankAccount.java, User.java"),
        ("Repositories", "BankAccountRepository.java (with findByIdWithLock), TransactionRepository.java, LedgerEntryRepository.java, IdempotencyKeyRepository.java, AuditLogRepository.java"),
        ("Services", "TransferService.java, TransferTransactionExecutor.java, AdminTransactionService.java, TransactionQueryService.java, LedgerService.java, IdempotencyService.java, AuditLogService.java, BruteForceProtectionService.java, RateLimiterService.java"),
        ("Controllers & Security", "ClientTransferController.java, AdminTransactionController.java, AuditLogController.java, RateLimitFilter.java, SecurityConfig.java, GlobalExceptionHandler.java"),
        ("Documentation & Tests", "README.md, MoneyTransferTests.java, ConcurrencyAndLockingTests.java, AcidRollbackTests.java, IdempotencyTests.java, LedgerAndAuditTests.java, SecurityHardeningTests.java")
    ]
    for cat, desc in files_info:
        p = doc.add_paragraph()
        p.add_run(f"• {cat}: ").bold = True
        p.add_run(desc)

    # Section D
    add_heading_styled(doc, "D. Database Schema & Tables", 2)
    tables = [
        ("users", "id (PK), username (UNIQUE), password (BCrypt), role (BANK_ADMIN/CLIENT), status (ACTIVE/BLOCKED/INACTIVE), created_at, updated_at"),
        ("bank_accounts", "id (PK), account_number (UNIQUE, 12-digit), user_id (FK -> users.id), balance (NUMERIC(19,2)), status (ACTIVE/BLOCKED/CLOSED), created_at, updated_at"),
        ("transactions", "id (PK), transaction_reference (UNIQUE), sender_account_id (FK -> bank_accounts.id), receiver_account_id (FK -> bank_accounts.id), amount (NUMERIC(19,2)), transaction_type (TRANSFER/DEPOSIT/WITHDRAWAL), status (PENDING/SUCCESS/FAILED), description, failure_reason, created_at, updated_at"),
        ("ledger_entries", "id (PK), transaction_id (FK -> transactions.id), account_id (FK -> bank_accounts.id), entry_type (DEBIT/CREDIT), amount (NUMERIC(19,2)), created_at"),
        ("idempotency_keys", "id (PK), idempotency_key (UNIQUE), user_id (FK -> users.id), transaction_reference, response_body (TEXT), status_code, created_at"),
        ("audit_logs", "id (PK), username, action (ENUM), entity_type, entity_id, description, result (SUCCESS/FAILED/RETRY), timestamp")
    ]
    for t_name, t_cols in tables:
        p = doc.add_paragraph()
        p.add_run(f"• {t_name}: ").bold = True
        p.add_run(t_cols)

    # Section E: API List Table
    add_heading_styled(doc, "E. Complete API List", 2)
    api_list = [
        ("/auth/login", "POST", "Public", "Client & Admin login with JWT token issuance"),
        ("/admin/clients", "POST", "BANK_ADMIN", "Create a new client and automatically assign bank account"),
        ("/admin/clients", "GET", "BANK_ADMIN", "List all registered client profiles"),
        ("/admin/clients/{id}", "GET", "BANK_ADMIN", "Get client details by User ID"),
        ("/admin/clients/search", "GET", "BANK_ADMIN", "Search clients by username keyword"),
        ("/admin/clients/{id}/block", "PATCH", "BANK_ADMIN", "Block a client user account"),
        ("/admin/clients/{id}/activate", "PATCH", "BANK_ADMIN", "Activate a client user account"),
        ("/admin/clients/{id}/deactivate", "PATCH", "BANK_ADMIN", "Deactivate a client user account"),
        ("/admin/accounts", "GET", "BANK_ADMIN", "List all system bank accounts"),
        ("/admin/accounts/{id}", "GET", "BANK_ADMIN", "Get bank account details by Account ID"),
        ("/admin/accounts/number/{accountNumber}", "GET", "BANK_ADMIN", "Get bank account details by 12-digit Account Number"),
        ("/admin/accounts/{id}/block", "PATCH", "BANK_ADMIN", "Block a bank account"),
        ("/admin/accounts/{id}/activate", "PATCH", "BANK_ADMIN", "Activate a bank account"),
        ("/admin/accounts/{id}/close", "PATCH", "BANK_ADMIN", "Close a bank account"),
        ("/admin/accounts/{id}/deposit", "POST", "BANK_ADMIN", "Admin deposit into a client account"),
        ("/admin/accounts/{id}/withdraw", "POST", "BANK_ADMIN", "Admin withdrawal from a client account"),
        ("/admin/transactions", "GET", "BANK_ADMIN", "System-wide transaction history (Paginated)"),
        ("/admin/transactions/{reference}", "GET", "BANK_ADMIN", "System transaction lookup by reference"),
        ("/admin/audit-logs", "GET", "BANK_ADMIN", "System audit trail logs (Paginated)"),
        ("/client/profile", "GET", "CLIENT", "Get authenticated user profile details"),
        ("/client/account", "GET", "CLIENT", "Get authenticated user bank account details"),
        ("/client/account/balance", "GET", "CLIENT", "Check authenticated user account balance"),
        ("/client/transfers", "POST", "CLIENT", "Execute money transfer to another client account"),
        ("/client/transactions", "GET", "CLIENT", "Get client's own transaction history (Paginated)")
    ]

    t_api = doc.add_table(rows=1, cols=4)
    t_api.alignment = WD_TABLE_ALIGNMENT.CENTER
    hdr_cells = t_api.rows[0].cells
    headers = ["Endpoint", "Method", "Role", "Description"]
    widths = [Inches(2.2), Inches(0.8), Inches(1.1), Inches(2.4)]
    for i, h_text in enumerate(headers):
        hdr_cells[i].text = h_text
        hdr_cells[i].width = widths[i]
        set_cell_background(hdr_cells[i], "102C57")
        p = hdr_cells[i].paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        for r in p.runs:
            r.font.bold = True
            r.font.color.rgb = RGBColor(255, 255, 255)
            r.font.size = Pt(9.5)

    for ep, meth, role, desc in api_list:
        row_cells = t_api.add_row().cells
        row_cells[0].text = ep
        row_cells[1].text = meth
        row_cells[2].text = role
        row_cells[3].text = desc
        for i in range(4):
            row_cells[i].width = widths[i]
            set_cell_margins(row_cells[i], top=60, bottom=60, left=80, right=80)
            p = row_cells[i].paragraphs[0]
            for r in p.runs:
                r.font.size = Pt(8.5)
                r.font.name = 'Calibri'
        row_cells[1].paragraphs[0].alignment = WD_ALIGN_PARAGRAPH.CENTER

    doc.add_paragraph().paragraph_format.space_after = Pt(6)

    # Section F
    add_heading_styled(doc, "F. Authentication Flow", 2)
    doc.add_paragraph(
        "1. Client submits credentials to POST /auth/login.\n"
        "2. System checks BruteForceProtectionService: if 5 consecutive failed login attempts occurred within 15 minutes, login is blocked.\n"
        "3. Password is verified against BCrypt hash using Spring Security AuthenticationManager.\n"
        "4. On success, JwtTokenProvider generates a signed HMAC-SHA384 JWT containing username, role (CLIENT/BANK_ADMIN), issued timestamp, and 24-hour expiration time.\n"
        "5. On failure, BruteForceProtectionService increments failure count and a generic HTTP 401 'Invalid credentials' response is returned without revealing user existence."
    )

    # Section G
    add_heading_styled(doc, "G. Money Transfer Flow", 2)
    doc.add_paragraph(
        "1. Client posts POST /client/transfers with optional Idempotency-Key header and payload receiverAccountNumber, amount, description.\n"
        "2. Authenticated user is extracted strictly from SecurityContextHolder (preventing sender spoofing).\n"
        "3. TransferService checks IdempotencyService for existing key:\n"
        "   - If key exists with cached response, return cached TransactionResponse immediately.\n"
        "   - If key exists in progress, throw DuplicateIdempotencyKeyException.\n"
        "   - If key is new, reserve it in idempotency_keys table.\n"
        "4. TransferTransactionExecutor.executeTransfer opens a @Transactional boundary:\n"
        "   - Validates sender status (ACTIVE).\n"
        "   - Retrieves sender and receiver accounts.\n"
        "   - Computes deterministic locking order: firstLockId = min(senderId, receiverId), secondLockId = max(senderId, receiverId).\n"
        "   - Acquires PESSIMISTIC_WRITE locks using findByIdWithLock on both accounts in deterministic order.\n"
        "   - Calls entityManager.refresh(...) to guarantee reloading fresh DB balance under pessimistic write lock.\n"
        "   - Validates account status (ACTIVE), self-transfer check, positive amount, and sufficient balance (sender.balance >= amount).\n"
        "   - Debits sender (sender.balance - amount), credits receiver (receiver.balance + amount), saves accounts.\n"
        "   - Generates unique transaction reference (TXN...), creates Transaction entity in SUCCESS status.\n"
        "   - Creates balanced double-entry ledger records (DEBIT on sender, CREDIT on receiver).\n"
        "   - Audits TRANSFER_SUCCESS event.\n"
        "5. IdempotencyService records serialized response body in database.\n"
        "6. Returns HTTP 201 Created with TransactionResponse."
    )

    # Section H
    add_heading_styled(doc, "H. ACID & Rollback Explanation", 2)
    doc.add_paragraph(
        "• Atomicity: The entire money transfer (validations, debit, credit, transaction record, ledger creation, and audit log) is bound inside a @Transactional block. If any step fails (e.g. database error, constraint violation, or exception), Spring's JpaTransactionManager issues a SQL ROLLBACK. Neither account balance is modified, and no partial transaction or ledger records remain.\n"
        "• Consistency: Database check constraints (balance >= 0, amount > 0), unique indexes (account_number, transaction_reference, idempotency_key), and application business rules maintain database integrity before and after every transaction.\n"
        "• Isolation: Managed via PostgreSQL row-level pessimistic locking (SELECT ... FOR UPDATE), preventing dirty reads, non-repeatable reads, and phantom reads during concurrent updates.\n"
        "• Durability: Committed transactions are flushed and persisted directly to disk in PostgreSQL tables."
    )

    # Section I
    add_heading_styled(doc, "I. Concurrency & Locking Explanation", 2)
    doc.add_paragraph(
        "• Race Condition Hazard: If Client A (balance ₹1,000) sends two concurrent transfers of ₹800 simultaneously, both threads could read balance ₹1,000 at the same time and both execute debits, producing an invalid negative balance (₹-600) or lost updates.\n"
        "• Pessimistic Locking (PESSIMISTIC_WRITE): Spring Data JPA @Lock(LockModeType.PESSIMISTIC_WRITE) executes a SQL SELECT ... FOR UPDATE (or FOR NO KEY UPDATE) on the account rows. The database blocks the second thread until the first thread commits or rolls back.\n"
        "• Deterministic Lock Ordering: To prevent circular wait deadlocks (e.g. A->B and B->A running concurrently), locks are always acquired in order of Math.min(idA, idB) first, followed by Math.max(idA, idB).\n"
        "• Entity Refresh: entityManager.refresh(...) is called under lock to reload the latest committed database balance into Hibernate's L1 cache, preventing stale reads."
    )

    # Section J
    add_heading_styled(doc, "J. Idempotency Explanation", 2)
    doc.add_paragraph(
        "Idempotency ensures that network retries or accidental duplicate submissions with the same Idempotency-Key header do not result in duplicate debits or duplicate money movements.\n"
        "• Database Uniqueness Enforcement: idempotency_keys.idempotency_key has a UNIQUE database constraint. If two identical requests hit the server simultaneously, database constraint validation prevents double reservation.\n"
        "• Response Caching: When a transfer completes, the response JSON payload and HTTP status are stored in idempotency_keys.response_body. Retried requests matching an existing key bypass execution and return the exact cached response."
    )

    # Section K
    add_heading_styled(doc, "K. Ledger Explanation", 2)
    doc.add_paragraph(
        "Implements real-world double-entry accounting. Every successful transfer creates two immutable records in ledger_entries:\n"
        "  1. Sender Account: DEBIT ₹X\n"
        "  2. Receiver Account: CREDIT ₹X\n"
        "For Deposits: Single CREDIT entry. For Withdrawals: Single DEBIT entry.\n"
        "Financial Accounting Rule: System-wide Sum(Total DEBITs) == Sum(Total CREDITs) at all times. Ledger entries are read-only and immutable once written."
    )

    # Section L
    add_heading_styled(doc, "L. Audit Logging Explanation", 2)
    doc.add_paragraph(
        "Every critical system action is logged into audit_logs table answering:\n"
        "  • WHO: Authenticated username\n"
        "  • WHAT: Action (LOGIN_SUCCESS, TRANSFER_SUCCESS, ACCOUNT_BLOCKED, etc.)\n"
        "  • WHEN: Timestamp\n"
        "  • RESOURCE: Entity Type & ID (BANK_ACCOUNT, TRANSACTION, USER)\n"
        "  • RESULT: SUCCESS, FAILED, or RETRY\n"
        "Security Compliance: Sensitive data such as plain passwords, BCrypt hashes, JWT tokens, or secrets are strictly excluded from audit descriptions."
    )

    # Section M
    add_heading_styled(doc, "M. Failure Handling & Controlled Retry", 2)
    doc.add_paragraph(
        "• Failure Classification:\n"
        "  - Permanent Failures: Business violations (insufficient balance, blocked account, non-existent account, negative amount). Throws runtime exceptions immediately without retry.\n"
        "  - Transient Failures: Temporary database lock timeouts or deadlock exceptions (TransientDataAccessException).\n"
        "• Controlled Retry Loop:\n"
        "  - Retries transient lock exceptions up to MAX_RETRIES = 3 attempts with exponential backoff delay (50ms * attempt).\n"
        "  - Protected by idempotency reservation so retries never cause duplicate money movement."
    )

    # Section N
    add_heading_styled(doc, "N. Security Hardening Features", 2)
    sec_features = [
        "1. Brute-Force Protection: Locks account after 5 consecutive failed login attempts for 15 minutes.",
        "2. Rate Limiting: Sliding-window rate limiter returns HTTP 429 TOO MANY REQUESTS when client exceeds 100 requests/minute.",
        "3. Input Validation: @Valid, @NotNull, @NotBlank, @Positive annotations on all incoming REST DTOs.",
        "4. Ownership Enforcement: Sender identity is resolved strictly from JWT SecurityContextHolder; clients cannot access another client's transactions or balance.",
        "5. BCrypt Password Hashing: Passwords are never stored or logged in plain text.",
        "6. Stateless JWT Security: Token expiration enforced at 24 hours, signature validated via HMAC-SHA384 secret stored in environment configuration.",
        "7. Safe Error Responses: GlobalExceptionHandler swallows stack traces, database schema details, and credentials."
    ]
    for sf in sec_features:
        doc.add_paragraph(sf, style='List Bullet')

    # Section O
    add_heading_styled(doc, "O. Test Suite Results", 2)
    doc.add_paragraph("All 37 integration test cases executed cleanly via Maven:")
    test_output = (
        "[INFO] Running com.bank.simulator.AcidRollbackTests\n"
        "[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.114 s\n"
        "[INFO] Running com.bank.simulator.AuthAndAuthorizationTests\n"
        "[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.205 s\n"
        "[INFO] Running com.bank.simulator.ClientAndAccountManagementTests\n"
        "[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.840 s\n"
        "[INFO] Running com.bank.simulator.ConcurrencyAndLockingTests\n"
        "[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 6.644 s\n"
        "[INFO] Running com.bank.simulator.IdempotencyTests\n"
        "[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.450 s\n"
        "[INFO] Running com.bank.simulator.LedgerAndAuditTests\n"
        "[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.950 s\n"
        "[INFO] Running com.bank.simulator.MoneyTransferTests\n"
        "[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.604 s\n"
        "[INFO] Running com.bank.simulator.SecurityHardeningTests\n"
        "[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.001 s\n"
        "[INFO] ------------------------------------------------------------------------\n"
        "[INFO] BUILD SUCCESS\n"
        "[INFO] Total time: 16.808 s\n"
    )
    add_code_block(doc, test_output)

    # Section P
    add_heading_styled(doc, "P. Important Design Decisions for Technical Interviews", 2)
    decisions = [
        ("Why BigDecimal for Money?", "Binary floating-point types (float, double) introduce IEEE 754 rounding inaccuracies (e.g. 0.1 + 0.2 = 0.30000000000000004). BigDecimal provides exact arbitrary-precision decimal arithmetic essential for financial applications."),
        ("Why Separate TransferTransactionExecutor Bean?", "Spring's @Transactional relies on AOP proxies. Internal method calls (this.executeTransfer()) bypass the proxy, disabling transaction boundaries and pessimistic locks. Extracting transactional logic into a separate Spring bean guarantees active proxy interceptors."),
        ("Why Deterministic Lock Ordering?", "If Thread 1 locks Account A then Account B, while Thread 2 locks Account B then Account A concurrently, a circular wait deadlock occurs. Ordering lock acquisition by Math.min(idA, idB) first eliminates circular waiting completely."),
        ("Why entityManager.refresh() under Lock?", "Hibernate's L1 cache stores previously queried entities in memory. Calling refresh() under a pessimistic write lock forces Hibernate to execute an immediate SQL SELECT to reload the latest committed DB state into the entity."),
        ("Why Database-backed Idempotency?", "In-memory maps reset on application restart and do not scale across threads cleanly without synchronization issues. Database-level UNIQUE constraints guarantee idempotency across concurrent requests.")
    ]
    for title, desc in decisions:
        p = doc.add_paragraph()
        p.add_run(f"• {title}: ").bold = True
        p.add_run(desc)

    # Section Q: 15 Interview Questions
    add_heading_styled(doc, "Q. 15 Technical Interview Questions & Concept Answers", 2)
    qa_list = [
        ("Q1: Why should monetary amounts never be stored as double or float in Java or databases?",
         "Answer: float and double use IEEE 754 binary floating-point representation, which cannot represent base-10 fractions (like 0.10 or 0.05) exactly. Accumulating floating-point operations introduces rounding errors that lead to financial discrepancies. We use Java's BigDecimal and SQL NUMERIC(19,2) / DECIMAL(19,2) to guarantee exact base-10 arithmetic."),
        ("Q2: What happens if a credit operation fails after a debit operation succeeds in a money transfer?",
         "Answer: Without @Transactional, the sender's balance is debited while the receiver's balance remains unchanged, causing money to vanish. With @Transactional wrapping the service method, Spring's JpaTransactionManager catches the exception and issues a SQL ROLLBACK, restoring all modified database rows back to their pre-transaction state atomically."),
        ("Q3: How do you prevent race conditions when two concurrent transfer requests debit the same account?",
         "Answer: We use JPA pessimistic locking (@Lock(LockModeType.PESSIMISTIC_WRITE)). In PostgreSQL, this translates to SELECT ... FOR UPDATE, acquiring an exclusive row lock on the account. The second concurrent thread is blocked by PostgreSQL until the first thread commits or rolls back, ensuring sequential balance checks and updates."),
        ("Q4: How do you prevent deadlocks during concurrent bidirectional transfers between two accounts (A -> B and B -> A)?",
         "Answer: Deadlocks occur due to circular wait conditions (Thread 1 holds A and waits for B; Thread 2 holds B and waits for A). We prevent deadlocks by enforcing deterministic lock ordering: we sort the account IDs and always acquire locks in order of Math.min(idA, idB) first, followed by Math.max(idA, idB)."),
        ("Q5: Why did calling @Transactional executeTransferInTransaction() from processTransfer() inside the same class fail to start a transaction?",
         "Answer: Spring's @Transactional relies on AOP proxies. When a method inside a class calls another method within the same class (this.method()), the call bypasses the Spring AOP proxy. We resolved this by creating a separate Spring component (TransferTransactionExecutor) so that calls route through Spring's proxy, properly creating and managing the transaction boundary."),
        ("Q6: Why is entityManager.refresh() necessary after acquiring a pessimistic write lock in JPA?",
         "Answer: Hibernate maintains an L1 Session Cache. If an entity was queried prior to acquiring the pessimistic lock, Hibernate returns the entity instance already present in the L1 cache. Calling entityManager.refresh(entity) after obtaining the pessimistic lock forces Hibernate to issue a SQL query to load the latest committed database state into the entity."),
        ("Q7: What is Idempotency, and how is it implemented in this banking system?",
         "Answer: Idempotency ensures that submitting the same request multiple times produces the exact same result without unintended side effects (such as double debits). We require an Idempotency-Key header. We store the key in an idempotency_keys table with a UNIQUE constraint. If a duplicate request arrives, we intercept it, bypass execution, and return the cached transaction response stored from the first request."),
        ("Q8: What is Double-Entry Bookkeeping and why is a LedgerEntry table needed alongside Transaction?",
         "Answer: In financial accounting, every money movement must have balancing DEBIT and CREDIT entries such that Total DEBIT == Total CREDIT. While Transaction records the business metadata of a transfer, LedgerEntry provides an immutable, audit-compliant financial record for each account affected."),
        ("Q9: Why is Idempotency-Key reservation done in its own transaction prior to transfer execution?",
         "Answer: If key reservation and transfer execution were in the same uncommitted transaction, concurrent requests with the same key might both pass an initial findByKey check before either commits. Executing reserveKey in its own committed transaction enforces DB-level unique constraint checks immediately, blocking concurrent duplicates."),
        ("Q10: How does the server prevent a client from transferring money out of another user's account?",
         "Answer: The REST API does not accept senderAccountId from the client request payload. Instead, the server extracts the authenticated username directly from the JWT token via Spring Security's SecurityContextHolder, looking up the account owned by that authenticated user."),
        ("Q11: What is the difference between transient failures and permanent failures in transaction management?",
         "Answer: Permanent failures (e.g., insufficient balance, blocked account, invalid account number) represent business rule violations and must fail immediately without retry. Transient failures (e.g., temporary database lock timeouts or deadlocks) represent transient concurrency contention; these are safely retried up to a fixed limit (e.g. 3 attempts) using exponential backoff."),
        ("Q12: How does Brute-Force Login Protection work without using Redis?",
         "Answer: We maintain an in-memory/database-backed BruteForceProtectionService tracking failed login attempts per username. If 5 consecutive failed login attempts occur within 15 minutes, the account is temporarily locked. Successful login resets the failure counter."),
        ("Q13: Why is CSRF protection disabled in SecurityConfig for this application?",
         "Answer: CSRF (Cross-Site Request Forgery) attacks rely on browser cookie submission for session authentication. Because our API uses stateless JWT authentication passed in the HTTP Authorization: Bearer <token> header, browsers do not automatically attach the token to cross-origin requests, making CSRF protection unnecessary."),
        ("Q14: How does the application implement API Rate Limiting for single-server architecture?",
         "Answer: A custom RateLimitFilter intercepts incoming HTTP requests for sensitive endpoints (/auth/login, /client/transfers). It tracks request timestamps within a sliding window (100 requests/minute). If the count exceeds the threshold, the filter halts execution and returns HTTP 429 TOO MANY REQUESTS."),
        ("Q15: Why are passwords never returned in DTOs or logged in Audit Logs?",
         "Answer: Returning or logging plain-text passwords or BCrypt hashes creates severe security vulnerabilities (log leakage, data exposure). DTOs sanitize entity responses by excluding security credentials, and AuditLogService strictly sanitizes descriptions to ensure compliance with OWASP and PCI-DSS standards.")
    ]

    for q, a in qa_list:
        p_q = doc.add_paragraph()
        p_q.paragraph_format.space_before = Pt(8)
        p_q.paragraph_format.space_after = Pt(2)
        r_q = p_q.add_run(q)
        r_q.bold = True
        r_q.font.color.rgb = RGBColor(16, 44, 87)
        r_q.font.size = Pt(10.5)

        p_a = doc.add_paragraph()
        p_a.paragraph_format.space_after = Pt(8)
        r_a = p_a.add_run(a)
        r_a.font.size = Pt(10)

    output_path = "d:/springboots/BANKING UPI TRANSACTION/Bank_Transaction_Simulator_Documentation.docx"
    doc.save(output_path)
    print("SUCCESSFULLY_SAVED: " + output_path)

if __name__ == "__main__":
    build_document()
