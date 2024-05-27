/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.message

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Context
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.model.TaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.model.TaskScheduleSlot
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleSlotAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getTaskIdentifier
import java.util.UUID

fun TaskScheduleAggregateAvro.toEntity(projectIdentifier: UUID) =
    TaskSchedule(
        identifier = buildAggregateIdentifier(),
        projectIdentifier = projectIdentifier,
        taskIdentifier = getTaskIdentifier(),
        end = getEnd()?.toLocalDateByMillis(),
        start = getStart()?.toLocalDateByMillis(),
        slots = getSlots().toEntities())

fun TaskScheduleAggregateAvro.buildContext(projectIdentifier: UUID) =
    Context(project = projectIdentifier, task = getTaskIdentifier())

private fun Collection<TaskScheduleSlotAvro>.toEntities() = map { it.toEntity() }

private fun TaskScheduleSlotAvro.toEntity() =
    TaskScheduleSlot(getDate().toLocalDateByMillis(), getDayCard().getIdentifier().toUUID())
