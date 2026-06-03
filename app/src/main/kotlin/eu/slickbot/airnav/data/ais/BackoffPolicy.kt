package eu.slickbot.airnav.data.ais

import kotlin.time.Duration

internal class BackoffPolicy(
  private val minBackoff: Duration,
  private val maxBackoff: Duration,
) {

  private var failureStreak: Long = 0

  fun nextDelay(translatedDuringSession: Long): Duration {
    failureStreak = if (translatedDuringSession > 0L) 0L else failureStreak + 1
    if (failureStreak == 0L) {
      return minBackoff.coerceAtLeast(Duration.ZERO)
    }
    var delay = minBackoff
    val doublings = (failureStreak - 1).coerceAtMost(8).toInt()
    repeat(doublings) {
      delay = (delay * 2).coerceAtMost(maxBackoff)
    }
    return delay.coerceIn(Duration.ZERO, maxBackoff)
  }
}
