@file:Suppress("MatchingDeclarationName")
/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.craft.craft

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftAggregateAvro
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.util.UUID
import jakarta.persistence.Embeddable

@Embeddable
data class CraftId(@get:JsonValue override val identifier: UUID) : Serializable, UuidIdentifiable {

  constructor(identifier: String) : this(identifier.toUUID())

  override fun toString(): String = identifier.toString()

  companion object {
    private const val serialVersionUID: Long = 9873468769827364L
    fun random() = CraftId(UUID.randomUUID())
  }
}

fun UUID.asCraftId() = CraftId(this)

fun CraftAggregateAvro.getIdentifier(): UUID =
    UUID.fromString(getAggregateIdentifier().getIdentifier())

fun CraftAggregateAvro.toCraftId(): CraftId = CraftId(getIdentifier())

fun AggregateIdentifierAvro.toCraftId(): CraftId = CraftId(UUID.fromString(getIdentifier()))
