package eu.slickbot.airnav.data.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoundedLruCacheTest {

    @Test
    fun `put then get returns value`() {
        val cache = BoundedLruCache<String, Int>(maxSize = 4)
        cache.put("a", 1)
        assertEquals(1, cache.get("a"))
    }

    @Test
    fun `LRU eviction respects access order`() {
        val cache = BoundedLruCache<String, Int>(maxSize = 2)
        cache.put("a", 1)
        cache.put("b", 2)
        cache.get("a")        // touch a
        cache.put("c", 3)     // evicts b
        assertEquals(1, cache.get("a"))
        assertNull(cache.get("b"))
        assertEquals(3, cache.get("c"))
    }

    @Test
    fun `getOrPut returns existing without calling loader`() {
        val cache = BoundedLruCache<String, Int>(maxSize = 4)
        cache.put("a", 1)
        var calls = 0
        val v = cache.getOrPut("a") { calls++; 99 }
        assertEquals(1, v)
        assertEquals(0, calls)
    }

    @Test
    fun `remove returns previous value`() {
        val cache = BoundedLruCache<String, Int>(maxSize = 4)
        cache.put("a", 1)
        assertEquals(1, cache.remove("a"))
        assertNull(cache.get("a"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `maxSize zero throws`() {
        BoundedLruCache<String, Int>(maxSize = 0)
    }
}
