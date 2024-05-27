/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.domain

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.MESSAGE
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID
import java.util.UUID.randomUUID

@Embeddable
data class MessageId(@get:JsonValue override val identifier: UUID = randomUUID()) :
    Serializable, UuidIdentifiable {

  constructor(identifier: String) : this(identifier.toUUID())

  override fun toString(): String = identifier.toString()

  companion object {
    private const val serialVersionUID: Long = -134866597220482783
  }
}

fun UUID.asMessageId() = MessageId(this)

fun String.asMessageId() = MessageId(this)

fun Set<UUID>.asMessageIds() = this.map { it.asMessageId() }.toSet()

fun Set<MessageId>.asUuidIds() = this.map { it.toUuid() }.toSet()

fun MessageId.toAggregateReference(): AggregateIdentifierAvro =
    AggregateIdentifierAvro.newBuilder()
        .setType(MESSAGE.name)
        .setIdentifier(this.identifier.toString())
        .setVersion(0)
        .build()
