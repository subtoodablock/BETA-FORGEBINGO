package com.forgebingo;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventDeduplicatorTest
{
    @Test
    public void suppressesImmediateDuplicateButAllowsLaterEvent()
    {
        MutableClock clock = new MutableClock();
        EventDeduplicator deduplicator = new EventDeduplicator(clock);
        String signature = EventDeduplicator.lootSignature("board", 42, 100, 10,
            Collections.singletonList(new ForgeBingoModels.LootItem(100, 1)));
        assertTrue(deduplicator.firstSeen(signature));
        assertFalse(deduplicator.firstSeen(signature));
        clock.millis += 2_001;
        assertTrue(deduplicator.firstSeen(signature));
    }

    @Test
    public void allowsIdenticalDropsFromDifferentMonstersOrGameTicks()
    {
        EventDeduplicator deduplicator = new EventDeduplicator(new MutableClock());
        ForgeBingoModels.LootItem item = new ForgeBingoModels.LootItem(4151, 1);

        assertTrue(deduplicator.firstSeen(EventDeduplicator.lootSignature("board", 415, 10, 100,
            Collections.singletonList(item))));
        assertTrue(deduplicator.firstSeen(EventDeduplicator.lootSignature("board", 415, 11, 100,
            Collections.singletonList(item))));
        assertTrue(deduplicator.firstSeen(EventDeduplicator.lootSignature("board", 415, 10, 101,
            Collections.singletonList(item))));
    }

    private static final class MutableClock extends Clock
    {
        private long millis;

        @Override
        public ZoneId getZone()
        {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone)
        {
            return this;
        }

        @Override
        public Instant instant()
        {
            return Instant.ofEpochMilli(millis);
        }

        @Override
        public long millis()
        {
            return millis;
        }
    }
}
