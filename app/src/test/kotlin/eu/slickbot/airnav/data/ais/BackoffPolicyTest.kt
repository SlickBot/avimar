package eu.slickbot.airnav.data.ais

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class BackoffPolicyTest {

  @Test
  fun `first failure waits minBackoff`() {
    val p = BackoffPolicy(minBackoff = 5.seconds, maxBackoff = 60.seconds)
    assertEquals(5.seconds, p.nextDelay(translatedDuringSession = 0L))
  }

  @Test
  fun `consecutive failures double the delay up to cap`() {
    val p = BackoffPolicy(minBackoff = 5.seconds, maxBackoff = 60.seconds)
    assertEquals(5.seconds, p.nextDelay(0L))
    assertEquals(10.seconds, p.nextDelay(0L))
    assertEquals(20.seconds, p.nextDelay(0L))
    assertEquals(40.seconds, p.nextDelay(0L))
    assertEquals(60.seconds, p.nextDelay(0L))
    assertEquals(60.seconds, p.nextDelay(0L))
  }

  @Test
  fun `successful session resets the streak`() {
    val p = BackoffPolicy(minBackoff = 5.seconds, maxBackoff = 60.seconds)
    p.nextDelay(0L); p.nextDelay(0L); p.nextDelay(0L)
    assertEquals(5.seconds, p.nextDelay(translatedDuringSession = 42L))
  }

  @Test
  fun `streak resumes from minBackoff after a successful session`() {
    val p = BackoffPolicy(minBackoff = 5.seconds, maxBackoff = 60.seconds)
    p.nextDelay(0L); p.nextDelay(0L)
    p.nextDelay(translatedDuringSession = 1L)
    assertEquals(5.seconds, p.nextDelay(0L))
    assertEquals(10.seconds, p.nextDelay(0L))
  }

  @Test
  fun `zero minBackoff yields zero delay always`() {
    val p = BackoffPolicy(minBackoff = Duration.ZERO, maxBackoff = 60.seconds)
    assertEquals(Duration.ZERO, p.nextDelay(0L))
    assertEquals(Duration.ZERO, p.nextDelay(0L))
    assertEquals(Duration.ZERO, p.nextDelay(0L))
  }
}
