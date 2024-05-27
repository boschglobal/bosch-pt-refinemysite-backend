/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.repository.existencecache

import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

/**
 * This cache stores references between cache name and search key and an entity identifier, so the
 * existence of the entity can be verified without the need of a query
 */
@RequestScope
@Component
open class ExistenceCache {

  private val entries: MutableMap<Any, Any> = HashMap()

  open fun size() = entries.size

  open fun put(cacheName: String, key: Any, identifier: Any) {
    entries[Pair(cacheName, key)] = identifier
  }

  open operator fun get(cacheName: String, key: Any): Any? = entries[Pair(cacheName, key)]

  open fun remove(identifier: Any): Any? =
      entries.forEach { (key, value) -> if (value == identifier) entries.remove(key) }

  open fun clear() = entries.clear()
}
