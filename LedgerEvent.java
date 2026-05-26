package pt.ledger;

import java.math.BigDecimal;
import java.time.Instant;

public final class LedgerEvent
{
    public enum EntryType { DEBIT, CREDIT }

    private final String    eventId;         // unique per event (UUID)
    private final String    transactionId;   // idempotency key (groups debit + credit pair)
    private final String    accountId;       // which account this entry touches
    private final EntryType entryType;       // DEBIT or CREDIT
    private final BigDecimal amount;         // always positive
    private final String    description;
    private final Instant   occurredAt;

    public LedgerEvent(String eventId,
                       String transactionId,
                       String accountId,
                       EntryType entryType,
                       BigDecimal amount,
                       String description,
                       Instant occurredAt)
    {
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be positive: " + amount);

        this.eventId       = eventId;
        this.transactionId = transactionId;
        this.accountId     = accountId;
        this.entryType     = entryType;
        this.amount        = amount;
        this.description   = description;
        this.occurredAt    = occurredAt;
    }

    public String     getEventId()       { return eventId; }
    public String     getTransactionId() { return transactionId; }
    public String     getAccountId()     { return accountId; }
    public EntryType  getEntryType()     { return entryType; }
    public BigDecimal getAmount()        { return amount; }
    public String     getDescription()   { return description; }
    public Instant    getOccurredAt()    { return occurredAt; }

    @Override
    public String toString()
    {
        return String.format("[%s] txn=%-36s  acct=%-20s  %-6s  %10.2f  \"%s\"",
                occurredAt, transactionId, accountId,
                entryType, amount, description);
    }
}
