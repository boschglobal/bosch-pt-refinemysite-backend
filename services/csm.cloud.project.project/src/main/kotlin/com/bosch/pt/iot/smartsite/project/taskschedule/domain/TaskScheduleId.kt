/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.domain

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID
import java.util.UUID.randomUUID

@Embeddable
data class TaskScheduleId(@get:JsonValue override val identifier: UUID = randomUUID()) :
    Serializable, UuidIdentifiable {

  constructor(identifier: String) : this(identifier.toUUID())

  override fun toString(): String = identifier.toString()

  companion object {
    private const val serialVersionUID: Long = 879917313254L
  }
}

fun UUID.asTaskScheduleId() = TaskScheduleId(this)

fun String.asTaskScheduleId() = TaskScheduleId(this)

fun Set<UUID>.asTaskScheduleIds() = this.map { it.asTaskScheduleId() }.toSet()

fun Set<TaskScheduleId>.asUuidIds() = this.map { it.toUuid() }.toSet()

fun TaskScheduleId.toAggregateReference(): AggregateIdentifierAvro =
    AggregateIdentifierAvro.newBuilder()
        .setType(ProjectmanagementAggregateTypeEnum.TASKSCHEDULE.name)
        .setIdentifier(this.identifier.toString())
        .setVersion(0)
        .build()
