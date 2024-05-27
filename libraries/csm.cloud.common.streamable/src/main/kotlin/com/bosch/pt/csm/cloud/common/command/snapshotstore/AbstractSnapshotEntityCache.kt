/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable

/**
 * A simple wrapper for a repository to cache items loaded with any repository call and make them
 * available for subsequent reads. This avoids redundant database calls in snapshot stores.
 *
 * @param <K> type of cache key (equals the external id type of the snapshot entity that is cached)
 * @param <V> type of cache value (equals the type of the snapshot entity that is cached)
 */
abstract class AbstractSnapshotEntityCache<K : UuidIdentifiable, V : AbstractSnapshotEntity<*, K>> {

  private val cache: MutableMap<K, V> = HashMap()

  open fun populateFromCall(block: () -> Collection<V>): Collection<V> =
      block.invoke().also { items ->
        items.associateBy { it.identifier }.apply { cache.putAll(this) }
      }

  open fun put(item: V): V = item.also { cache[item.identifier] = item }

  open fun get(identifier: K): V? =
      cache[identifier] ?: loadOneFromDatabase(identifier).also { if (it != null) put(it) }

  open fun remove(identifier: K): V? = cache.remove(identifier)

  open fun clear() = cache.clear()

  protected abstract fun loadOneFromDatabase(identifier: K): V?
}
