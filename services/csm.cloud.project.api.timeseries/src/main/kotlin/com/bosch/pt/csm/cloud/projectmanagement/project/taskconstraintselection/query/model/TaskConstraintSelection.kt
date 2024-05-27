/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.domain.TaskConstraintSelectionId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

const val TASK_CONSTRAINT_SELECTION_PROJECTION = "TaskConstraintSelectionProjection"

@Document(TASK_CONSTRAINT_SELECTION_PROJECTION)
@TypeAlias(TASK_CONSTRAINT_SELECTION_PROJECTION)
data class TaskConstraintSelection(
    @Id val identifier: TaskConstraintSelectionId,
    val version: Long,
    val project: ProjectId,
    val task: TaskId,
    val constraints: List<TaskConstraintEnum>,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
    val history: List<TaskConstraintSelectionVersion>
)

data class TaskConstraintSelectionVersion(
    val version: Long,
    val constraints: List<TaskConstraintEnum>,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
)
