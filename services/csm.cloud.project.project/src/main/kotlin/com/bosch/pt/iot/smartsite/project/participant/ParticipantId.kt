/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.util.UUID
import jakarta.persistence.Embeddable

@Embeddable
data class ParticipantId(@get:JsonValue override val identifier: UUID = UUID.randomUUID()) :
    Serializable, UuidIdentifiable {

  constructor(identifier: String) : this(identifier.toUUID())

  override fun toString(): String = identifier.toString()

  companion object {
    private const val serialVersionUID: Long = 873825386254L
  }
}

fun UUID.asParticipantId() = ParticipantId(this)
fun Set<UUID>.asParticipantIds() = this.map { ParticipantId(it) }.toSet()
fun String.asParticipantId() = ParticipantId(this)
fun ParticipantId.toAggregateReference(): AggregateIdentifierAvro =
    AggregateIdentifierAvro.newBuilder()
        .setType(ProjectmanagementAggregateTypeEnum.PARTICIPANT.name)
        .setIdentifier(this.identifier.toString())
        .setVersion(0)
        .build()