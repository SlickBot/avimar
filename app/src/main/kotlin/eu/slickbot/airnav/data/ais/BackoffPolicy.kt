package eu.slickbot.airnav.data.ais

/**
 * Failure-streak exponential backoff with a cap. A session that
 * successfully translated at least one AIS message resets the streak —
 * so a working stream that closes after hours doesn't trigger long
 * reconnect waits the way fixed-doubling backoff does.
 */
internal class BackoffPolicy(
  private val minBackoffMs: Long,
  private val maxBackoffMs: Long,
) {

  private var failureStreak: Long = 0

  /**
   * Compute the delay to wait before the NEXT reconnect attempt, given the
   * number of AIS messages translated during the just-ended session.
   */
  fun nextDelayMs(translatedDuringSession: Long): Long {
    failureStreak = if (translatedDuringSession > 0L) 0L else failureStreak + 1
    if (failureStreak == 0L) {
      return minBackoffMs.coerceAtLeast(0L)
    }
    var delay = minBackoffMs
    val doublings = (failureStreak - 1).coerceAtMost(8).toInt()
    repeat(doublings) {
      delay = (delay * 2).coerceAtMost(maxBackoffMs)
    }
    return delay.coerceIn(0L, maxBackoffMs)
  }
}
