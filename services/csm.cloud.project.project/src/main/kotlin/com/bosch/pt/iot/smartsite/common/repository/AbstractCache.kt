/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.repository

import com.google.common.collect.Sets

/**
 * A simple cache to avoid redundant database calls. Typically used in [RequestScope] to ensure
 * every cache value is loaded only once per request.
 *
 * @param <K> type of the cache key
 * @param <V> type of the cache value </V></K>
 */
abstract class AbstractCache<K, V> {

  protected val cache: MutableMap<K, V> = HashMap()

  /**
   * Returns the cached values for the given keys, or loads them from the database in case of a
   * cache miss. If only some values are uncached, only these values will be loaded from the
   * database while the remaining values will be served from cache.
   *
   * @param keys the keys of values to be returned
   * @return the values for the given keys
   */
  operator fun get(keys: Set<K>): Collection<V> {
    val cachedKeys: Set<K> = cache.keys
    val uncachedKeys: Set<K> = Sets.difference(keys, cachedKeys)

    if (uncachedKeys.isNotEmpty()) {
      cache.putAll(associateByCacheKey(loadFromDatabase(uncachedKeys)))
    }

    return getCachedValues(keys)
  }

  /** Deletes the cache value for the given key. */
  open fun invalidateCacheKey(key: K) = cache.remove(key)

  /**
   * Loads the values for the given cache keys from the database. This method will be invoked in
   * case of a cache miss to populate the cache.
   *
   * @param keys the keys of values to be loaded
   * @return the values loaded from the database
   */
  abstract fun loadFromDatabase(keys: Set<K>): Set<V>

  /** Extracts the cache key from the given value. */
  protected abstract fun getCacheKey(value: V): K

  private fun getCachedValues(keys: Set<K>): Collection<V> =
      cache.filterKeys { keys.contains(it) }.values

  private fun associateByCacheKey(values: Set<V>): Map<K, V> =
      values.associateBy { getCacheKey(it) }
}
