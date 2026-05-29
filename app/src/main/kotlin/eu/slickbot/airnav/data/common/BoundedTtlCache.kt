package eu.slickbot.airnav.data.common

import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Thread-safe cache with bounded size (LRU eviction) and per-entry TTL.
 *
 * Not a high-throughput cache — uses a single synchronized LinkedHashMap. Suitable
 * for provider-lookup caches where contention is low.
 */
class BoundedTtlCache<K : Any, V : Any>(
  private val maxSize: Int,
  private val ttl: Duration,
  private val clock: Clock = Clock.systemUTC(),
) {

  init {
    require(maxSize > 0) { "maxSize must be positive, was $maxSize" }
    require(!ttl.isNegative) { "ttl must not be negative, was $ttl" }
  }

  private data class Entry<V>(val value: V, val storedAt: Instant)

  private val map: LinkedHashMap<K, Entry<V>> =
    object : LinkedHashMap<K, Entry<V>>(16, 0.75f, /* accessOrder = */ true) {
      override fun removeEldestEntry(eldest: Map.Entry<K, Entry<V>>): Boolean = size > maxSize
    }

  private val lock = Any()

  fun get(key: K): V? {
    synchronized(lock) {
      val entry = map[key] ?: return null
      if (isExpired(entry)) {
        map.remove(key)
        return null
      }
      return entry.value
    }
  }

  fun put(key: K, value: V) {
    synchronized(lock) {
      map[key] = Entry(value, Instant.now(clock))
    }
  }

  fun getOrPut(key: K, loader: () -> V): V {
    synchronized(lock) {
      val existing = map[key]
      if (existing != null && !isExpired(existing)) {
        return existing.value
      }
      val value = loader()
      map[key] = Entry(value, Instant.now(clock))
      return value
    }
  }

  fun remove(key: K): V? {
    synchronized(lock) {
      return map.remove(key)?.value
    }
  }

  fun clear() {
    synchronized(lock) { map.clear() }
  }

  fun size(): Int {
    synchronized(lock) { return map.size }
  }

  private fun isExpired(entry: Entry<V>): Boolean {
    if (ttl.isZero) return false
    return Duration.between(entry.storedAt, Instant.now(clock)) > ttl
  }
}
