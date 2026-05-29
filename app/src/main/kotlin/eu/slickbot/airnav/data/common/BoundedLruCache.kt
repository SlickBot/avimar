package eu.slickbot.airnav.data.common

/**
 * LRU-bounded cache with no TTL. Useful for holding expensive-to-load immutable
 * resources where eviction is driven purely by memory pressure.
 */
class BoundedLruCache<K : Any, V : Any>(
  private val maxSize: Int,
) {

  init {
    require(maxSize > 0) { "maxSize must be positive, was $maxSize" }
  }

  private val map: LinkedHashMap<K, V> =
    object : LinkedHashMap<K, V>(16, 0.75f, /* accessOrder = */ true) {
      override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean = size > maxSize
    }

  private val lock = Any()

  fun get(key: K): V? {
    synchronized(lock) { return map[key] }
  }

  fun put(key: K, value: V) {
    synchronized(lock) { map[key] = value }
  }

  fun getOrPut(key: K, loader: () -> V): V {
    synchronized(lock) {
      val existing = map[key]
      if (existing != null) return existing
      val value = loader()
      map[key] = value
      return value
    }
  }

  fun remove(key: K): V? {
    synchronized(lock) { return map.remove(key) }
  }

  fun clear() {
    synchronized(lock) { map.clear() }
  }

  fun size(): Int {
    synchronized(lock) { return map.size }
  }
}
