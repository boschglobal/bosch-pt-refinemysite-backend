/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.domain.TaskConstraintId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

const val TASK_CONSTRAINT_PROJECTION = "TaskConstraintProjection"

@Document(TASK_CONSTRAINT_PROJECTION)
@TypeAlias(TASK_CONSTRAINT_PROJECTION)
data class TaskConstraint(
    @Id val identifier: TaskConstraintId,
    val version: Long,
    val project: ProjectId,
    val key: TaskConstraintEnum,
    val name: String? = null,
    val active: Boolean,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
    val history: List<TaskConstraintVersion>
)

data class TaskConstraintVersion(
    val version: Long,
    val key: TaskConstraintEnum,
    val name: String? = null,
    val active: Boolean,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
)

enum class TaskConstraintEnum(private val constraint: String, val id: UUID) {
  MATERIAL("MATERIAL", "a3d5a050-133a-4b0c-a378-24876ac9cb56".toUUID()),
  RESOURCES("LABOR", "72b27529-01bb-4a28-8cda-72ff9138e427".toUUID()),
  INFORMATION("INFORMATION", "96beca1a-6a32-480f-8384-a54c5582f7a5".toUUID()),
  EQUIPMENT("EQUIPMENT", "08fdfcd7-452d-4298-8fff-eda8b819811c".toUUID()),
  PRELIMINARY_WORK("PRELIMINARY_WORK", "cd687dfa-8ef3-4ea2-bcf4-a891db17b2b0".toUUID()),
  SAFE_WORKING_ENVIRONMENT("SAFETY", "b4c482a8-d3d4-4cd6-b99a-a7329d6bcba1".toUUID()),
  EXTERNAL_FACTORS("EXTERNAL_FACTORS", "bde22aba-af62-40e7-9cb1-9253c1f25716".toUUID()),
  COMMON_UNDERSTANDING("CLARIFICATION_NEEDED", "690be360-b370-48e1-b56a-ef574bef3b34".toUUID()),
  CUSTOM1("CUSTOM1", "ea26dd91-fe5a-4e19-a27a-a4aafb285269".toUUID()),
  CUSTOM2("CUSTOM2", "13527565-ba8a-4ee7-b6fd-30a8a08ca304".toUUID()),
  CUSTOM3("CUSTOM3", "0cf302f6-858d-48f3-9e17-8168adfe32fc".toUUID()),
  CUSTOM4("CUSTOM4", "65775aac-e404-416e-acfd-8c5694337902".toUUID());

  companion object {
    const val KEY_PREFIX: String = "TASK_CONSTRAINT_"
  }

  val isCustom: Boolean
    get() = this == CUSTOM1 || this == CUSTOM2 || this == CUSTOM3 || this == CUSTOM4

  val shortKey: String
    get() = this.constraint

  val key: String
    get() = "${KEY_PREFIX}${this.constraint}"

  val messageKey: String
    get() = "${TaskConstraintEnum::class.simpleName}_$this"

  val timestamp: Long
    get() = 0L
}
