package com.forgebingo;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EventDeduplicator
{
    private static final long RETENTION_MILLIS = 2_000L;
    private static final int MAX_ENTRIES = 100;
    private final Clock clock;
    private final Map<String, Long> recent = new LinkedHashMap<>();

    EventDeduplicator(Clock clock)
    {
        this.clock = clock;
    }

    static String lootSignature(String boardId, int npcId, int npcIndex, int gameTick,
        List<ForgeBingoModels.LootItem> items)
    {
        StringBuilder signature = new StringBuilder(boardId)
            .append(':').append(npcId)
            .append(':').append(npcIndex)
            .append(':').append(gameTick);
        if (items != null)
        {
            for (ForgeBingoModels.LootItem item : items)
            {
                if (item != null)
                {
                    signature.append(':').append(item.itemId).append('x').append(item.quantity);
                }
            }
        }
        return signature.toString();
    }

    synchronized boolean firstSeen(String signature)
    {
        long now = clock.millis();
        recent.entrySet().removeIf(entry -> now - entry.getValue() > RETENTION_MILLIS);
        if (recent.containsKey(signature))
        {
            return false;
        }
        recent.put(signature, now);
        while (recent.size() > MAX_ENTRIES)
        {
            recent.remove(recent.keySet().iterator().next());
        }
        return true;
    }

    synchronized void clear()
    {
        recent.clear();
    }
}
