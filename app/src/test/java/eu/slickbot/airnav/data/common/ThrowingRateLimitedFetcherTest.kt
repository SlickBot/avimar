package eu.slickbot.airnav.data.common

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.time.Duration
import java.time.Instant

class ThrowingRateLimitedFetcherTest {

    @Test
    fun `fetch passes through under the limit`() {
        val clock = FakeClock(Instant.parse("2026-01-01T00:00:00Z"))
        val fetcher = ThrowingRateLimitedFetcher<Int, String>(
            fetcher = { "v$it" },
            limits = listOf(RateLimit(maxUnits = 2, window = Duration.ofSeconds(1))),
            clock = clock,
        )
        assertEquals("v1", fetcher.fetch(1))
        assertEquals("v2", fetcher.fetch(2))
    }

    @Test
    fun `fetch throws when limit exceeded`() {
        val clock = FakeClock(Instant.parse("2026-01-01T00:00:00Z"))
        val fetcher = ThrowingRateLimitedFetcher<Int, String>(
            fetcher = { "v$it" },
            limits = listOf(RateLimit(maxUnits = 2, window = Duration.ofSeconds(1))),
            clock = clock,
        )
        fetcher.fetch(1)
        fetcher.fetch(2)
        try {
            fetcher.fetch(3)
            fail("expected RateLimitException")
        } catch (_: RateLimitException) {
            // expected
        }
    }

    @Test
    fun `entries outside window stop counting`() {
        val clock = FakeClock(Instant.parse("2026-01-01T00:00:00Z"))
        val fetcher = ThrowingRateLimitedFetcher<Int, String>(
            fetcher = { "v$it" },
            limits = listOf(RateLimit(maxUnits = 2, window = Duration.ofSeconds(1))),
            clock = clock,
        )
        fetcher.fetch(1)
        fetcher.fetch(2)
        clock.advance(Duration.ofSeconds(2))
        assertEquals("v3", fetcher.fetch(3))
    }

    @Test
    fun `minInterval convenience constructor enforces spacing`() {
        val clock = FakeClock(Instant.parse("2026-01-01T00:00:00Z"))
        val fetcher = ThrowingRateLimitedFetcher<Int, String>(
            fetcher = { "v$it" },
            minInterval = Duration.ofMillis(500),
            clock = clock,
        )
        fetcher.fetch(1)
        try {
            fetcher.fetch(2)
            fail("expected RateLimitException")
        } catch (_: RateLimitException) { }
        clock.advance(Duration.ofMillis(501))
        assertEquals("v2", fetcher.fetch(2))
    }

    @Test
    fun `costOf greater than one consumes more budget`() {
        val clock = FakeClock(Instant.parse("2026-01-01T00:00:00Z"))
        val fetcher = ThrowingRateLimitedFetcher<Int, String>(
            fetcher = { "v$it" },
            limits = listOf(RateLimit(maxUnits = 5, window = Duration.ofSeconds(1))),
            costOf = { it },  // cost == query value
            clock = clock,
        )
        fetcher.fetch(3)  // used = 3
        fetcher.fetch(2)  // used = 5
        try {
            fetcher.fetch(1)  // would be 6, over limit
            fail("expected RateLimitException")
        } catch (_: RateLimitException) { }
    }
}
