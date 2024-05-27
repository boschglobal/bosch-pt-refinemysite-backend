/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.service

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.domain.asTaskConstraintId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TaskConstraint
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TaskConstraintMapper
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TaskConstraintVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.repository.TaskConstraintRepository
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.message.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class TaskConstraintProjector(private val repository: TaskConstraintRepository) {

  fun onTaskConstraintEvent(aggregate: TaskConstraintCustomizationAggregateAvro) {
    val existingConstraint =
        repository.findOneByIdentifier(aggregate.getIdentifier().asTaskConstraintId())

    if (existingConstraint == null ||
        aggregate.aggregateIdentifier.version > existingConstraint.version) {
      (existingConstraint?.updateFromTaskConstraintAggregate(aggregate)
              ?: aggregate.toNewProjection())
          .apply { repository.save(this) }
    }
  }

  fun onTaskConstraintDeletedEvent(aggregate: TaskConstraintCustomizationAggregateAvro) {
    val constraint = repository.findOneByIdentifier(aggregate.getIdentifier().asTaskConstraintId())
    if (constraint != null && !constraint.deleted) {
      val newVersion =
          constraint.history
              .last()
              .copy(
                  deleted = true,
                  version = aggregate.getVersion(),
                  eventDate =
                      aggregate.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis(),
                  eventAuthor =
                      aggregate.auditingInformation.lastModifiedBy.identifier.toUUID().asUserId())

      repository.save(
          TaskConstraintMapper.INSTANCE.fromTaskConstraintVersion(
              newVersion,
              constraint.identifier,
              constraint.project,
              constraint.history.toMutableList().also { it.add(newVersion) }))
    }
  }

  private fun TaskConstraintCustomizationAggregateAvro.toNewProjection(): TaskConstraint {
    val constraintVersion = this.newTaskConstraintVersion()

    return TaskConstraintMapper.INSTANCE.fromTaskConstraintVersion(
        taskConstraintVersion = constraintVersion,
        identifier = aggregateIdentifier.identifier.toUUID().asTaskConstraintId(),
        project = project.identifier.toUUID().asProjectId(),
        history = listOf(constraintVersion))
  }

  private fun TaskConstraint.updateFromTaskConstraintAggregate(
      aggregate: TaskConstraintCustomizationAggregateAvro
  ): TaskConstraint {
    val constraintVersion = aggregate.newTaskConstraintVersion()

    return TaskConstraintMapper.INSTANCE.fromTaskConstraintVersion(
        taskConstraintVersion = constraintVersion,
        identifier = this.identifier,
        project = this.project,
        history = this.history.toMutableList().also { it.add(constraintVersion) })
  }

  private fun TaskConstraintCustomizationAggregateAvro.newTaskConstraintVersion():
      TaskConstraintVersion {
    val isNew = this.aggregateIdentifier.version == 0L
    val auditUser: UserId
    val auditDate: LocalDateTime
    if (isNew) {
      auditUser = UserId(this.auditingInformation.createdBy.identifier.toUUID())
      auditDate = this.auditingInformation.createdDate.toLocalDateTimeByMillis()
    } else {
      auditUser = UserId(this.auditingInformation.lastModifiedBy.identifier.toUUID())
      auditDate = this.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis()
    }

    return TaskConstraintVersion(
        version = this.aggregateIdentifier.version,
        key = TaskConstraintEnum.valueOf(this.key.name),
        name = this.name,
        active = this.active,
        eventAuthor = auditUser,
        eventDate = auditDate)
  }
}
