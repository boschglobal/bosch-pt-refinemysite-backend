/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.api

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID
import java.util.UUID.randomUUID

@Embeddable
data class UserId(@get:JsonValue override val identifier: UUID = randomUUID()) :
    Serializable, UuidIdentifiable {

  constructor(identifier: String) : this(identifier.toUUID())

  override fun toString(): String = identifier.toString()

  companion object {
    private const val serialVersionUID: Long = 325479765432L
  }
}

fun UUID.asUserId() = UserId(this)

fun String.asUserId() = UserId(this)

fun Set<UUID>.asUserIds() = this.map { UserId(it) }.toSet()

fun Set<UserId>.asUuidIds() = this.map { it.toUuid() }.toSet()

fun UserId.toAggregateReference(): AggregateIdentifierAvro =
    AggregateIdentifierAvro.newBuilder()
        .setType("USER")
        .setIdentifier(this.identifier.toString())
        .setVersion(0)
        .build()

fun AggregateIdentifierAvro.toUserId(): UserId = UserId(identifier.toUUID())

fun AggregateIdentifier.toUserId(): UserId = UserId(identifier)
