/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.quickfilter.domain

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.util.UUID
import java.util.UUID.randomUUID

data class QuickFilterId(@get:JsonValue override val identifier: UUID = randomUUID()) :
    Serializable, UuidIdentifiable {

  constructor(identifier: String) : this(identifier.toUUID())

  override fun toString(): String = identifier.toString()

  companion object {
    private const val serialVersionUID: Long = 879917313254L
  }
}

fun UUID.asQuickFilterId() = QuickFilterId(this)

fun String.asQuickFilterId() = QuickFilterId(this)

fun Set<UUID>.asQuickFilterIds() = this.map { it.asQuickFilterId() }.toSet()

fun Set<QuickFilterId>.asUuidIds() = this.map { it.toUuid() }.toSet()
