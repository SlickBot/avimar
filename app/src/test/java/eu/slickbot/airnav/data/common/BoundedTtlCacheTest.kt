package eu.slickbot.airnav.data.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class BoundedTtlCacheTest {

    @Test
    fun `put then get returns value when fresh`() {
        val cache = BoundedTtlCache<String, Int>(maxSize = 4, ttl = Duration.ofMinutes(1))
        cache.put("a", 1)
        assertEquals(1, cache.get("a"))
    }

    @Test
    fun `get returns null after ttl elapses`() {
        val clock = FakeClock(Instant.parse("2026-01-01T00:00:00Z"))
        val cache = BoundedTtlCache<String, Int>(maxSize = 4, ttl = Duration.ofSeconds(10), clock = clock)
        cache.put("a", 1)
        clock.advance(Duration.ofSeconds(11))
        assertNull(cache.get("a"))
    }

    @Test
    fun `expired entry is removed on read`() {
        val clock = FakeClock(Instant.parse("2026-01-01T00:00:00Z"))
        val cache = BoundedTtlCache<String, Int>(maxSize = 4, ttl = Duration.ofSeconds(10), clock = clock)
        cache.put("a", 1)
        clock.advance(Duration.ofSeconds(11))
        cache.get("a")
        assertEquals(0, cache.size())
    }

    @Test
    fun `LRU eviction when over capacity`() {
        val cache = BoundedTtlCache<String, Int>(maxSize = 2, ttl = Duration.ofMinutes(1))
        cache.put("a", 1)
        cache.put("b", 2)
        cache.get("a")            // touch a
        cache.put("c", 3)         // should evict b (least recently used)
        assertEquals(1, cache.get("a"))
        assertNull(cache.get("b"))
        assertEquals(3, cache.get("c"))
    }

    @Test
    fun `getOrPut returns existing fresh value without calling loader`() {
        val cache = BoundedTtlCache<String, Int>(maxSize = 4, ttl = Duration.ofMinutes(1))
        cache.put("a", 1)
        var calls = 0
        val v = cache.getOrPut("a") { calls++; 99 }
        assertEquals(1, v)
        assertEquals(0, calls)
    }

    @Test
    fun `getOrPut calls loader and stores when missing`() {
        val cache = BoundedTtlCache<String, Int>(maxSize = 4, ttl = Duration.ofMinutes(1))
        val v = cache.getOrPut("a") { 42 }
        assertEquals(42, v)
        assertEquals(42, cache.get("a"))
    }

    @Test
    fun `ttl zero means never expire`() {
        val clock = FakeClock(Instant.parse("2026-01-01T00:00:00Z"))
        val cache = BoundedTtlCache<String, Int>(maxSize = 4, ttl = Duration.ZERO, clock = clock)
        cache.put("a", 1)
        clock.advance(Duration.ofHours(24))
        assertEquals(1, cache.get("a"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `maxSize zero throws`() {
        BoundedTtlCache<String, Int>(maxSize = 0, ttl = Duration.ofMinutes(1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative ttl throws`() {
        BoundedTtlCache<String, Int>(maxSize = 1, ttl = Duration.ofSeconds(-1))
    }
}
