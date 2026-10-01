package com.github.premtechworks.synqvia.service

import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

data class RetryPolicy(
    val initialDelayMs: Long = 2000L,
    val maxDelayMs: Long = 30000L,
    val multiplier: Double = 2.0,
    val maxAttempts: Int = 5,
    val jitterFactor: Double = 0.2
) {
    fun computeDelayWithJitter(attempt: Int): Long {
        val baseDelay = (initialDelayMs * multiplier.pow(attempt.coerceAtLeast(0).toDouble())).toLong()
        val capped = min(baseDelay, maxDelayMs)
        val jitterRange = (capped * jitterFactor).toLong()
        val jitter = if (jitterRange > 0) Random.nextLong(-jitterRange, jitterRange + 1) else 0L
        return (capped + jitter).coerceIn(initialDelayMs / 2, maxDelayMs + jitterRange)
    }
}
