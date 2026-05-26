package pt.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class DoubleEntryLedger
{
	private final EventStore store;
	private final AccountProjection projection;

	public DoubleEntryLedger()
	{
		this.store = new EventStore();
		this.projection = new AccountProjection(store);
	}

	public void createAccount(String accountId,
		String name,
		AccountProjection.AccountType type)
	{
		projection.registerAccount(accountId, name, type);
	}

	public List<LedgerEvent> post(String transactionId,
		String debitAccountId,
		String creditAccountId,
		BigDecimal amount,
		String description)
	{
		if(amount.compareTo(BigDecimal.ZERO) <= 0)
			throw new IllegalArgumentException("Amount must be positive.");

		if(debitAccountId.equals(creditAccountId))
			throw new IllegalArgumentException("Debit and credit accounts must differ.");

		Instant now = Instant.now();

		LedgerEvent debitEvent = new LedgerEvent(
			UUID.randomUUID().toString(),
			transactionId,
			debitAccountId,
			LedgerEvent.EntryType.DEBIT,
			amount,
			description,
			now
		);

		LedgerEvent creditEvent = new LedgerEvent(
			UUID.randomUUID().toString(),
			transactionId,
			creditAccountId,
			LedgerEvent.EntryType.CREDIT,
			amount,
			description,
			now
		);

		return store.append(transactionId, List.of(debitEvent, creditEvent));
	}

	public BigDecimal getBalance(String accountId)
	{
		return projection.getBalance(accountId);
	}

	public List<LedgerEvent> getAuditTrail(String accountId)
	{
		return projection.getAuditTrail(accountId);
	}

	public boolean isCommitted(String transactionId)
	{
		return store.isCommitted(transactionId);
	}

	public List<LedgerEvent> getTransaction(String transactionId)
	{
		return store.getEventsForTransaction(transactionId);
	}

	public boolean verifyLedgerBalanced()
	{
		BigDecimal totalDebits = BigDecimal.ZERO;
		BigDecimal totalCredits = BigDecimal.ZERO;

		for(LedgerEvent e : store.getGlobalLog())
		{
			if(e.getEntryType() == LedgerEvent.EntryType.DEBIT)
				totalDebits = totalDebits.add(e.getAmount());
			else
				totalCredits = totalCredits.add(e.getAmount());
		}

		System.out.printf("Ledger check  →  Σ debits = %10.2f  |  Σ credits = %10.2f  |  balanced = %s%n",
			totalDebits, totalCredits,
			totalDebits.compareTo(totalCredits) == 0 ? "YES" : "NO");

		return totalDebits.compareTo(totalCredits) == 0;
	}
	public void printAccountSummary(String accountId)
	{
		AccountProjection.AccountMeta meta = projection.getMeta(accountId);
		BigDecimal balance = getBalance(accountId);

		System.out.printf("%n┌─ %s (%s / %s) ──────────────────────────────────────%n",
			meta.getName(), meta.getAccountId(), meta.getType());
		System.out.printf("│  Balance: %,.2f%n", balance);
		System.out.println("│  Audit trail:");

		List<LedgerEvent> trail = getAuditTrail(accountId);
		if(trail.isEmpty())
		{
			System.out.println("│    (no events)");
		}
		else
		{
			for(LedgerEvent e : trail)
				System.out.printf("│    %s%n", e);
		}
		System.out.println("└───────────────────────────────────────────────────────────────");
	}

	public static void main(String[] args)
	{
		DoubleEntryLedger ledger = new DoubleEntryLedger();

		// --- 1. Create accounts --------------------------------------------------
		ledger.createAccount("cash", "Cash", AccountProjection.AccountType.ASSET);
		ledger.createAccount("revenue", "Revenue", AccountProjection.AccountType.REVENUE);
		ledger.createAccount("bank", "Bank Account", AccountProjection.AccountType.ASSET);
		ledger.createAccount("accounts_pay", "Accounts Payable", AccountProjection.AccountType.LIABILITY);
		ledger.createAccount("expenses", "Operating Expenses", AccountProjection.AccountType.EXPENSE);

		// --- 2. Post normal transactions -----------------------------------------
		System.out.println("=== Posting transactions ===\n");

		// Customer pays $500 cash → debit cash, credit revenue
		String txn1 = UUID.randomUUID().toString();
		ledger.post(txn1, "cash", "revenue", new BigDecimal("500.00"), "Customer payment - Invoice #1001");
		System.out.println("Posted txn1 (customer payment $500)");

		// Deposit $500 cash into bank → debit bank, credit cash
		String txn2 = UUID.randomUUID().toString();
		ledger.post(txn2, "bank", "cash", new BigDecimal("500.00"), "Cash deposit to bank");
		System.out.println("Posted txn2 (deposit to bank $500)");

		// Pay supplier $200 from bank → debit accounts_payable, credit bank
		String txn3 = UUID.randomUUID().toString();
		ledger.post(txn3, "accounts_pay", "bank", new BigDecimal("200.00"), "Supplier payment - PO #55");
		System.out.println("Posted txn3 (supplier payment $200)");

		// Record office expense $80 → debit expenses, credit bank
		String txn4 = UUID.randomUUID().toString();
		ledger.post(txn4, "expenses", "bank", new BigDecimal("80.00"), "Office supplies");
		System.out.println("Posted txn4 (office expenses $80)");

		// --- 3. Idempotency demo -------------------------------------------------
		System.out.println("\n=== Idempotency check ===\n");

		System.out.println("Is txn1 already committed? " + ledger.isCommitted(txn1));

		System.out.println("Re-posting txn1 with same transactionId (should be no-op)...");
		List<LedgerEvent> reposted = ledger.post(txn1, "cash", "revenue",
			new BigDecimal("500.00"), "Customer payment - Invoice #1001");
		System.out.println("Re-post returned " + reposted.size() + " original events (not duplicated).");

		// Confirm balance unchanged after idempotent re-post
		System.out.printf("Cash balance after idempotent re-post: %.2f (should still be 0.00)%n",
			ledger.getBalance("cash"));

		// --- 4. Print audit trails -----------------------------------------------
		System.out.println("\n=== Account Summaries & Audit Trails ===");
		ledger.printAccountSummary("cash");
		ledger.printAccountSummary("revenue");
		ledger.printAccountSummary("bank");
		ledger.printAccountSummary("accounts_pay");
		ledger.printAccountSummary("expenses");

		// --- 5. Verify double-entry invariant ------------------------------------
		System.out.println("\n=== Ledger Balance Verification ===\n");
		ledger.verifyLedgerBalanced();
	}
}
