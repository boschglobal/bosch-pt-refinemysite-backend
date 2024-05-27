/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.model

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.PROJECT_STATE
import com.bosch.pt.csm.cloud.projectmanagement.project.common.model.ShardedByProjectIdentifier
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

@Document(PROJECT_STATE)
@TypeAlias("Task")
data class Task(
    @Id val identifier: AggregateIdentifier,
    override val projectIdentifier: UUID,
    val name: String,
    val status: TaskStatusEnum,
    val description: String? = null,
    val location: String? = null,
    val craftIdentifier: UUID? = null,
    val assigneeIdentifier: UUID? = null,
    val workAreaIdentifier: UUID? = null
) : ShardedByProjectIdentifier

enum class TaskStatusEnum {
  DRAFT,
  OPEN,
  STARTED,
  CLOSED,
  ACCEPTED
}
