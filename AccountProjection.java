package pt.ledger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class AccountProjection
{
    public enum AccountType { ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE }

    /** Lightweight account descriptor (metadata only — no mutable balance). */
    public static final class AccountMeta
    {
        private final String      accountId;
        private final String      name;
        private final AccountType type;

        public AccountMeta(String accountId, String name, AccountType type)
        {
            this.accountId = accountId;
            this.name      = name;
            this.type      = type;
        }

        public String      getAccountId() { return accountId; }
        public String      getName()      { return name; }
        public AccountType getType()      { return type; }

        @Override
        public String toString()
        {
            return String.format("Account{id='%s', name='%s', type=%s}",
                    accountId, name, type);
        }
    }


    private final EventStore                  store;
    private final Map<String, AccountMeta>   accounts = new ConcurrentHashMap<>();

    public AccountProjection(EventStore store)
    {
        this.store = store;
    }

    public void registerAccount(String accountId, String name, AccountType type)
    {
        accounts.putIfAbsent(accountId, new AccountMeta(accountId, name, type));
    }

    public BigDecimal getBalance(String accountId)
    {
        requireKnown(accountId);

        return store.getEventsForAccount(accountId)
                .stream()
                .map(e -> e.getEntryType() == LedgerEvent.EntryType.CREDIT
                        ?  e.getAmount()
                        :  e.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<LedgerEvent> getAuditTrail(String accountId)
    {
        requireKnown(accountId);
        return store.getEventsForAccount(accountId);
    }

    public Set<String> getAccountIds()
    {
        return accounts.keySet();
    }

    public AccountMeta getMeta(String accountId)
    {
        requireKnown(accountId);
        return accounts.get(accountId);
    }

    private void requireKnown(String accountId)
    {
        if (!accounts.containsKey(accountId))
            throw new IllegalArgumentException("Unknown account: " + accountId);
    }
}
