package eu.slickbot.airnav.data.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class MinIntervalRateLimiterTest {

  private class FakeNanoClock(start: Long = 0L) {
    var nanos: Long = start
    fun advance(d: Duration) { nanos += d.toNanos() }
  }

  @Test
  fun `first call waits nothing`() {
    val clock = FakeNanoClock()
    val limiter = MinIntervalRateLimiter(
      minTimeout = Duration.ofMillis(100),
      nanoClock = { clock.nanos },
      sleeper = { },
    )
    val waited = limiter.awaitPermit()
    assertEquals(Duration.ZERO, waited)
  }

  @Test
  fun `second call waits up to minTimeout`() {
    val clock = FakeNanoClock()
    val sleeps = mutableListOf<Duration>()
    val limiter = MinIntervalRateLimiter(
      minTimeout = Duration.ofMillis(100),
      nanoClock = { clock.nanos },
      sleeper = { d ->
        sleeps += d
        clock.advance(d)   // simulate the wait
      },
    )

    limiter.awaitPermit()
    clock.advance(Duration.ofMillis(30))  // 70ms still to wait
    val waited = limiter.awaitPermit()

    assertEquals(1, sleeps.size)
    assertEquals(Duration.ofMillis(70), sleeps.single())
    assertEquals(Duration.ofMillis(70), waited)
  }

  @Test
  fun `call after gap exceeding minTimeout does not sleep`() {
    val clock = FakeNanoClock()
    val sleeps = mutableListOf<Duration>()
    val limiter = MinIntervalRateLimiter(
      minTimeout = Duration.ofMillis(100),
      nanoClock = { clock.nanos },
      sleeper = { d -> sleeps += d; clock.advance(d) },
    )

    limiter.awaitPermit()
    clock.advance(Duration.ofMillis(500))
    val waited = limiter.awaitPermit()

    assertTrue(sleeps.isEmpty())
    assertEquals(Duration.ZERO, waited)
  }

  @Test
  fun `zero minTimeout never sleeps`() {
    val clock = FakeNanoClock()
    val sleeps = mutableListOf<Duration>()
    val limiter = MinIntervalRateLimiter(
      minTimeout = Duration.ZERO,
      nanoClock = { clock.nanos },
      sleeper = { sleeps += it },
    )

    repeat(5) { limiter.awaitPermit() }

    assertTrue(sleeps.isEmpty())
  }
}
