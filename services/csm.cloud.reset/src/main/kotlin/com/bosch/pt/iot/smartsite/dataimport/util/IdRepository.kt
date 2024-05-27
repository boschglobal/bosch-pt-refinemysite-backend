/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.util

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Component

@Component
class IdRepository(private val mapping: MutableMap<TypedId, UUID> = ConcurrentHashMap()) {

  operator fun get(id: TypedId): UUID? = mapping[id]

  fun containsId(id: TypedId): Boolean = get(id) != null

  fun reset() = mapping.clear()

  fun store(id: TypedId, uuid: UUID) {
    mapping[id] = uuid
  }
}
