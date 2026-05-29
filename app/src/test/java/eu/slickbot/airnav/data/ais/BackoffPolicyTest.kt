package eu.slickbot.airnav.data.ais

import org.junit.Assert.assertEquals
import org.junit.Test

class BackoffPolicyTest {

  @Test
  fun `first failure waits minBackoff`() {
    val p = BackoffPolicy(minBackoffMs = 5_000L, maxBackoffMs = 60_000L)
    assertEquals(5_000L, p.nextDelayMs(translatedDuringSession = 0L))
  }

  @Test
  fun `consecutive failures double the delay up to cap`() {
    val p = BackoffPolicy(minBackoffMs = 5_000L, maxBackoffMs = 60_000L)
    assertEquals(5_000L, p.nextDelayMs(0L))
    assertEquals(10_000L, p.nextDelayMs(0L))
    assertEquals(20_000L, p.nextDelayMs(0L))
    assertEquals(40_000L, p.nextDelayMs(0L))
    assertEquals(60_000L, p.nextDelayMs(0L))
    assertEquals(60_000L, p.nextDelayMs(0L))
  }

  @Test
  fun `successful session resets the streak`() {
    val p = BackoffPolicy(minBackoffMs = 5_000L, maxBackoffMs = 60_000L)
    p.nextDelayMs(0L); p.nextDelayMs(0L); p.nextDelayMs(0L)
    assertEquals(5_000L, p.nextDelayMs(translatedDuringSession = 42L))
  }

  @Test
  fun `streak resumes from minBackoff after a successful session`() {
    val p = BackoffPolicy(minBackoffMs = 5_000L, maxBackoffMs = 60_000L)
    p.nextDelayMs(0L); p.nextDelayMs(0L)
    p.nextDelayMs(translatedDuringSession = 1L)
    assertEquals(5_000L, p.nextDelayMs(0L))
    assertEquals(10_000L, p.nextDelayMs(0L))
  }

  @Test
  fun `zero minBackoff yields zero delay always`() {
    val p = BackoffPolicy(minBackoffMs = 0L, maxBackoffMs = 60_000L)
    assertEquals(0L, p.nextDelayMs(0L))
    assertEquals(0L, p.nextDelayMs(0L))
    assertEquals(0L, p.nextDelayMs(0L))
  }
}
