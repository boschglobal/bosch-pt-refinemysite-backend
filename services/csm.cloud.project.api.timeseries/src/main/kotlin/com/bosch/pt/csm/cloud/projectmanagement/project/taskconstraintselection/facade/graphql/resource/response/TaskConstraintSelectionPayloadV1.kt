/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.graphql.resource.response

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TaskConstraintEnum
import java.time.LocalDateTime
import java.util.UUID

data class TaskConstraintSelectionPayloadV1(
    val id: UUID,
    val version: Long,
    val eventDate: LocalDateTime,

    // Only for internal resolution
    val projectId: ProjectId,
    val constraintIds: List<TaskConstraintEnum>
)
