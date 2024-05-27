/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.message

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.EventInformation
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintSelection
import java.util.UUID
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionAggregateAvro as TaskConstraintSelectionAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventAvro as TaskConstraintSelectionEventAvro

fun TaskConstraintSelectionAggregateAvro.buildAggregateIdentifier() =
    AggregateIdentifier(
        type = getAggregateIdentifier().getType(),
        identifier = getAggregateIdentifier().getIdentifier().toUUID(),
        version = getAggregateIdentifier().getVersion())

fun TaskConstraintSelectionAggregateAvro.buildTaskIdentifier() = getTask().getIdentifier().toUUID()

fun TaskConstraintSelectionAggregateAvro.buildConstraintSelection(projectIdentifier: UUID) =
    TaskConstraintSelection(
        aggregateIdentifier = buildAggregateIdentifier(),
        projectIdentifier = projectIdentifier,
        taskIdentifier = buildTaskIdentifier(),
        actions = getActions().map { TaskConstraintEnum.valueOf(it.name) }.toMutableList())

fun TaskConstraintSelectionAggregateAvro.getLastModifiedDate() =
    getAuditingInformation().getLastModifiedDate().toInstantByMillis()

fun TaskConstraintSelectionAggregateAvro.getLastModifiedByUserIdentifier() =
    getAuditingInformation().getLastModifiedBy().getIdentifier().toUUID()

fun TaskConstraintSelectionAggregateAvro.buildEventInformation(
    taskConstraintSelectionEventAvro: TaskConstraintSelectionEventAvro
) =
    EventInformation(
        name = taskConstraintSelectionEventAvro.getName().name,
        date = taskConstraintSelectionEventAvro.getLastModifiedDate(),
        user = taskConstraintSelectionEventAvro.getLastModifiedByUserIdentifier())
