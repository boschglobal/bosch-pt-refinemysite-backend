/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.buildNotificationIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.buildObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.project.task.model.Task
import com.bosch.pt.csm.cloud.projectmanagement.project.task.model.TaskStatusEnum
import com.bosch.pt.csm.cloud.projectmanagement.task.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getAssigneeIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getCraftIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getWorkAreaIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import java.util.UUID

fun TaskAggregateAvro.buildObjectReference() = aggregateIdentifier.buildObjectReference()

fun TaskAggregateAvro.buildNotificationIdentifier(recipientIdentifier: UUID) =
    aggregateIdentifier.buildNotificationIdentifier(recipientIdentifier)

fun TaskAggregateAvro.buildTask() =
    Task(
        identifier = buildAggregateIdentifier(),
        projectIdentifier = getProjectIdentifier(),
        name = name,
        description = description,
        location = location,
        craftIdentifier = getCraftIdentifier(),
        assigneeIdentifier = assignee?.let { getAssigneeIdentifier() },
        status = TaskStatusEnum.valueOf(status.toString()),
        workAreaIdentifier = workarea?.let { getWorkAreaIdentifier() })
