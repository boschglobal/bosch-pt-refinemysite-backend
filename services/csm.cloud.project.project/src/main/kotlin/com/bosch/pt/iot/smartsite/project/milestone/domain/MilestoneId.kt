/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

@file:Suppress("MatchingDeclarationName")

package com.bosch.pt.iot.smartsite.project.milestone.domain

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.MILESTONE
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.util.UUID
import java.util.UUID.randomUUID
import jakarta.persistence.Embeddable

@Embeddable
data class MilestoneId(@get:JsonValue override val identifier: UUID = randomUUID()) :
    Serializable, UuidIdentifiable {

  constructor(identifier: String) : this(identifier.toUUID())

  override fun toString(): String = identifier.toString()

  companion object {
    private const val serialVersionUID: Long = 879917313254L
  }
}

fun UUID.asMilestoneId() = MilestoneId(this)

fun String.asMilestoneId() = MilestoneId(this)

fun MilestoneId.toAggregateReference(): AggregateIdentifierAvro =
    AggregateIdentifierAvro.newBuilder()
        .setType(MILESTONE.name)
        .setIdentifier(identifier.toString())
        .setVersion(0)
        .build()
