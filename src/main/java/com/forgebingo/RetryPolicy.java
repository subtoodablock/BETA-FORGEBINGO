package com.forgebingo;

final class RetryPolicy
{
    private RetryPolicy()
    {
    }

    static long pollDelaySeconds(int consecutiveFailures, long initialDelaySeconds, long maximumDelaySeconds)
    {
        int exponent = Math.min(Math.max(0, consecutiveFailures - 1), 4);
        return Math.min(maximumDelaySeconds, initialDelaySeconds * (1L << exponent));
    }

    static long lootDelaySeconds(int attempt, long maximumDelaySeconds)
    {
        int exponent = Math.min(Math.max(0, attempt), 6);
        return Math.min(maximumDelaySeconds, 5L * (1L << exponent));
    }
}
