package pt.ledger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class EventStore
{
    private final List<LedgerEvent> globalLog = new CopyOnWriteArrayList<>();
    private final Map<String, List<LedgerEvent>> idempotencyIndex = new ConcurrentHashMap<>();
    public List<LedgerEvent> append(String transactionId, List<LedgerEvent> events)
    {
        List<LedgerEvent> existing = idempotencyIndex.putIfAbsent(
                transactionId,
                Collections.unmodifiableList(new ArrayList<>(events))
        );

        if (existing != null)
        {
            return existing;
        }

        globalLog.addAll(events);
        return idempotencyIndex.get(transactionId);
    }

    public List<LedgerEvent> getEventsForAccount(String accountId)
    {
        return globalLog.stream()
                .filter(e -> e.getAccountId().equals(accountId))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<LedgerEvent> getEventsForTransaction(String transactionId)
    {
        return idempotencyIndex.getOrDefault(
                transactionId, Collections.emptyList());
    }

    public List<LedgerEvent> getGlobalLog()
    {
        return Collections.unmodifiableList(new ArrayList<>(globalLog));
    }

    public boolean isCommitted(String transactionId)
    {
        return idempotencyIndex.containsKey(transactionId);
    }

    public int totalEventCount() { return globalLog.size(); }
}
