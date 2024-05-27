/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.rest.resource.response

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.domain.TaskConstraintSelectionId

data class TaskConstraintSelectionResource(
    val id: TaskConstraintSelectionId,
    val version: Long,
    val project: ProjectId,
    val task: TaskId,
    val key: String,
    val deleted: Boolean = false,
    val eventTimestamp: Long
)
