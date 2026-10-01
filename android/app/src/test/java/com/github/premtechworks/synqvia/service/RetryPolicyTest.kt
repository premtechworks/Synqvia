package com.github.premtechworks.synqvia.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryPolicyTest {

    @Test
    fun defaultValues_areCorrect() {
        val policy = RetryPolicy()
        assertEquals(5, policy.maxAttempts)
        assertEquals(2000L, policy.initialDelayMs)
        assertEquals(30000L, policy.maxDelayMs)
        assertEquals(2.0, policy.multiplier, 0.001)
        assertEquals(0.2, policy.jitterFactor, 0.001)
    }

    @Test
    fun computeDelayWithJitter_attemptZero_isNearInitialDelay() {
        val policy = RetryPolicy(initialDelayMs = 2000L, jitterFactor = 0.2)
        val delay = policy.computeDelayWithJitter(0)
        // With jitter +/- 20% on 2000: range is [1600, 2400]
        assertTrue("Delay $delay should be >= 1000", delay >= 1000L)
        assertTrue("Delay $delay should be <= 2500", delay <= 2500L)
    }

    @Test
    fun computeDelayWithJitter_increasesWithAttempt() {
        val policy = RetryPolicy(initialDelayMs = 1000L, multiplier = 2.0, jitterFactor = 0.0)
        val d0 = policy.computeDelayWithJitter(0)
        val d1 = policy.computeDelayWithJitter(1)
        val d2 = policy.computeDelayWithJitter(2)

        assertEquals(1000L, d0)
        assertEquals(2000L, d1)
        assertEquals(4000L, d2)
    }

    @Test
    fun computeDelayWithJitter_isCappedAtMaxDelay() {
        val policy = RetryPolicy(initialDelayMs = 1000L, maxDelayMs = 10000L, multiplier = 2.0, jitterFactor = 0.0)
        val delay = policy.computeDelayWithJitter(10)
        assertEquals(10000L, delay)
    }
}
