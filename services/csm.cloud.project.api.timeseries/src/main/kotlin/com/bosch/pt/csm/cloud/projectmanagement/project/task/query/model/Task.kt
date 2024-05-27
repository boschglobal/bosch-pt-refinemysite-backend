/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.craft.domain.ProjectCraftId
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.domain.ParticipantId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.domain.WorkAreaId
import com.bosch.pt.csm.cloud.projectmanagement.translation.shared.model.TranslatableEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

const val TASK_PROJECTION = "TaskProjection"

@Document(TASK_PROJECTION)
@TypeAlias(TASK_PROJECTION)
data class Task(
    @Id val identifier: TaskId,
    val version: Long,
    val project: ProjectId,
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val craft: ProjectCraftId,
    val assignee: ParticipantId? = null,
    val status: TaskStatusEnum,
    val editDate: LocalDateTime? = null,
    val workArea: WorkAreaId? = null,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
    val history: List<TaskVersion>
)

data class TaskVersion(
    val version: Long,
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val craft: ProjectCraftId,
    val assignee: ParticipantId? = null,
    val status: TaskStatusEnum,
    val editDate: LocalDateTime? = null,
    val workArea: WorkAreaId? = null,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
)

enum class TaskStatusEnum(private val status: String) : TranslatableEnum {
  DRAFT("DRAFT"),
  OPEN("OPEN"),
  STARTED("IN_PROGRESS"),
  CLOSED("DONE"),
  ACCEPTED("ACCEPTED");

  companion object {
    const val KEY_PREFIX: String = "TASK_STATUS_"
  }

  val shortKey: String
    get() = this.status

  override val key: String
    get() = "${KEY_PREFIX}${this.status}"

  override val messageKey: String
    get() = "${TaskStatusEnum::class.simpleName}_$this"
}
