/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.rest.resource.response

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.domain.TaskConstraintId

data class TaskConstraintResource(
    val id: TaskConstraintId,
    val version: Long,
    val project: ProjectId,
    val key: String,
    val active: Boolean,
    val language: String,
    val name: String,
    val deleted: Boolean = false,
    val eventTimestamp: Long
)
