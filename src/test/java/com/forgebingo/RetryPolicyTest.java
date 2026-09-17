package com.forgebingo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RetryPolicyTest
{
    @Test
    public void boardPollingBacksOffAndCapsAtFiveMinutes()
    {
        assertEquals(30, RetryPolicy.pollDelaySeconds(1, 30, 300));
        assertEquals(60, RetryPolicy.pollDelaySeconds(2, 30, 300));
        assertEquals(240, RetryPolicy.pollDelaySeconds(4, 30, 300));
        assertEquals(300, RetryPolicy.pollDelaySeconds(20, 30, 300));
    }

    @Test
    public void lootRetriesBackOffAndCapAtFiveMinutes()
    {
        assertEquals(5, RetryPolicy.lootDelaySeconds(0, 300));
        assertEquals(40, RetryPolicy.lootDelaySeconds(3, 300));
        assertEquals(300, RetryPolicy.lootDelaySeconds(20, 300));
    }
}
