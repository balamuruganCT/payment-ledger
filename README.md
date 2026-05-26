# payment-ledger

A small in-memory double-entry ledger demo implemented in Java.

Overview
- Simple event-sourced double-entry ledger.
- Idempotent transaction posting based on a transaction id (idempotency key).
- In-memory event store with account projection for balances and audit trails.

Project structure (key files)
- `DoubleEntryLedger.java` — main API and demo `main()` entry point.
- `EventStore.java` — in-memory append-only event log and idempotency index.
- `AccountProjection.java` — projection to compute balances and audit trails from events.
- `LedgerEvent.java` — immutable event model.

Requirements
- Java 11+ (JDK 11 recommended). The code uses `List.of(...)` and unmodifiable collectors; Java 11 or later is the safest choice.

Compile and run (command-line)
From the project root (where the `.java` files live):

Alternative (recursive compile):

What the demo does
- Creates several accounts: `cash`, `bank`, `revenue`, `accounts_pay`, `expenses`
- Posts a few transactions (customer payment, deposit, supplier payment, expense)
- Demonstrates idempotent posting: re-posting with the same transaction ID is a no-op
- Prints account summaries and audit trails
- Verifies that total debits equal total credits (double-entry invariant)

Notes and suggestions
- This is an in-memory demo: everything is stored in-memory (no persistence).
- If you want to turn this into a packaged application, add a build tool (Maven/Gradle), create proper source layout (`src/main/java/pt/ledger/...`) and a module manifest or shade a runnable jar.
- If you prefer to run from an IDE, create a Java project in IntelliJ IDEA and run `pt.ledger.DoubleEntryLedger.main()`.
