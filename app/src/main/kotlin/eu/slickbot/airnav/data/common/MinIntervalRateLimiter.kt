package eu.slickbot.airnav.data.common

import java.time.Duration
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Enforces minimum spacing between consecutive operations.
 */
class MinIntervalRateLimiter(
  private val minTimeout: Duration,
  private val nanoClock: () -> Long = System::nanoTime,
  private val sleeper: (Duration) -> Unit = Companion::sleepFor,
) {

  private val lock = ReentrantLock()
  private var nextAllowedAtNanos: Long? = null

  fun awaitPermit(): Duration {
    if (minTimeout.isZero || minTimeout.isNegative) {
      return Duration.ZERO
    }

    val minTimeoutNanos = minTimeout.toNanos()
    var waitedNanos = 0L
    while (true) {
      val waitNanos = lock.withLock {
        val now = nanoClock()
        val nextAllowedAt = nextAllowedAtNanos
        if (nextAllowedAt == null || now >= nextAllowedAt) {
          nextAllowedAtNanos = now + minTimeoutNanos
          0L
        } else {
          nextAllowedAt - now
        }
      }

      if (waitNanos <= 0L) {
        return Duration.ofNanos(waitedNanos)
      }

      sleeper(Duration.ofNanos(waitNanos))
      waitedNanos += waitNanos
    }
  }

  private companion object {
    fun sleepFor(duration: Duration) {
      if (duration.isZero || duration.isNegative) {
        return
      }
      try {
        val ms = duration.toMillis()
        val extraNanos = duration.minusMillis(ms).toNanos().toInt()
        Thread.sleep(ms, extraNanos)
      } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        throw RateLimitException("Rate-limited request interrupted", e)
      }
    }
  }
}
