/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Context
import com.bosch.pt.csm.cloud.projectmanagement.project.task.model.Task
import com.bosch.pt.csm.cloud.projectmanagement.project.task.model.TaskStatusEnum
import com.bosch.pt.csm.cloud.projectmanagement.task.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getAssigneeIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getCraftIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getTaskIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getWorkAreaIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import java.util.UUID

fun TaskAggregateAvro.toEntity(projectIdentifier: UUID) =
    Task(
        identifier = buildAggregateIdentifier(),
        projectIdentifier = projectIdentifier,
        name = getName(),
        description = getDescription(),
        location = getLocation(),
        craftIdentifier = getCraftIdentifier(),
        assigneeIdentifier = getAssigneeIdentifier(),
        status = TaskStatusEnum.valueOf(getStatus().name),
        workAreaIdentifier = getWorkAreaIdentifier())

fun TaskAggregateAvro.buildContext(projectIdentifier: UUID) =
    Context(project = projectIdentifier, task = getTaskIdentifier())
