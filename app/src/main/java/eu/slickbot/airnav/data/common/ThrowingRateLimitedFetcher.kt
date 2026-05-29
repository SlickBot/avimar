package eu.slickbot.airnav.data.common

import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Generic fetch wrapper that throws [RateLimitException] when any configured
 * rate limit would be exceeded. Supports multiple sliding-window limits.
 *
 * Ported from drone-swarm-v9 commander/data/common/ThrowingRateLimitedFetcher.kt.
 */
class ThrowingRateLimitedFetcher<Q, R>(
  private val fetcher: (Q) -> R,
  private val limits: List<RateLimit>,
  private val costOf: (Q) -> Int = { 1 },
  private val clock: Clock = Clock.systemUTC(),
) {

  constructor(
    fetcher: (Q) -> R,
    minInterval: Duration,
    clock: Clock = Clock.systemUTC(),
  ) : this(
    fetcher = fetcher,
    limits = if (minInterval.isZero || minInterval.isNegative) emptyList()
    else listOf(RateLimit(maxUnits = 1, window = minInterval)),
    clock = clock,
  )

  private data class Entry(val time: Instant, val cost: Int)

  private val lock = Any()
  private val history = ArrayDeque<Entry>()

  fun fetch(query: Q): R {
    val now = Instant.now(clock)
    val cost = costOf(query)
    synchronized(lock) {
      for (limit in limits) {
        val windowStart = now.minus(limit.window)
        val used = history.asSequence()
          .filter { it.time > windowStart }
          .sumOf { it.cost }
        if (used + cost > limit.maxUnits) {
          val oldest = history.firstOrNull { it.time > windowStart }
          val retryAfter = if (oldest != null) {
            Duration.between(now, oldest.time.plus(limit.window))
              .coerceAtLeast(Duration.ZERO)
          } else {
            Duration.ZERO
          }
          throw RateLimitException(
            "Rate-limited (${limit.maxUnits}/${limit.window}). Retry after ${retryAfter.toMillis()} ms.",
          )
        }
      }
      history.addLast(Entry(now, cost))
      val maxWindow = limits.maxOfOrNull { it.window } ?: Duration.ZERO
      val cutoff = now.minus(maxWindow)
      while (history.isNotEmpty() && !history.first().time.isAfter(cutoff)) {
        history.removeFirst()
      }
    }
    return fetcher(query)
  }
}
