/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.service

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.asTaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.domain.asTaskConstraintSelectionId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.model.TaskConstraintSelection
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.model.TaskConstraintSelectionMapper
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.model.TaskConstraintSelectionVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.repository.TaskConstraintSelectionRepository
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class TaskConstraintSelectionProjector(private val repository: TaskConstraintSelectionRepository) {

  fun onTaskConstraintSelectionEvent(
      aggregate: TaskActionSelectionAggregateAvro,
      projectId: ProjectId
  ) {
    val existingSelection =
        repository.findOneByIdentifier(
            aggregate.aggregateIdentifier.identifier.toUUID().asTaskConstraintSelectionId())

    if (existingSelection == null ||
        aggregate.aggregateIdentifier.version > existingSelection.version) {
      (existingSelection?.updateFromTaskConstraintSelectionAggregate(aggregate)
              ?: aggregate.toNewProjection(projectId))
          .apply { repository.save(this) }
    }
  }

  fun onTaskConstraintSelectionDeletedEvent(aggregate: TaskActionSelectionAggregateAvro) {
    val constraintSelection =
        repository.findOneByIdentifier(
            aggregate.aggregateIdentifier.identifier.toUUID().asTaskConstraintSelectionId())

    if (constraintSelection != null && !constraintSelection.deleted) {
      val newVersion =
          constraintSelection.history
              .last()
              .copy(
                  deleted = true,
                  version = aggregate.aggregateIdentifier.version,
                  eventDate =
                      aggregate.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis(),
                  eventAuthor =
                      aggregate.auditingInformation.lastModifiedBy.identifier.toUUID().asUserId())

      repository.save(
          TaskConstraintSelectionMapper.INSTANCE.fromTaskConstraintSelectionVersion(
              newVersion,
              constraintSelection.identifier,
              constraintSelection.project,
              constraintSelection.task,
              constraintSelection.history.toMutableList().also { it.add(newVersion) }))
    }
  }

  private fun TaskActionSelectionAggregateAvro.toNewProjection(
      project: ProjectId
  ): TaskConstraintSelection {
    val constraintSelectionVersion = this.newTaskConstraintSelectionVersion()

    return TaskConstraintSelectionMapper.INSTANCE.fromTaskConstraintSelectionVersion(
        taskConstraintSelectionVersion = constraintSelectionVersion,
        identifier = aggregateIdentifier.identifier.toUUID().asTaskConstraintSelectionId(),
        project = project,
        task = task.identifier.toUUID().asTaskId(),
        history = listOf(constraintSelectionVersion))
  }

  private fun TaskConstraintSelection.updateFromTaskConstraintSelectionAggregate(
      aggregate: TaskActionSelectionAggregateAvro
  ): TaskConstraintSelection {
    val constraintSelectionVersion = aggregate.newTaskConstraintSelectionVersion()

    return TaskConstraintSelectionMapper.INSTANCE.fromTaskConstraintSelectionVersion(
        taskConstraintSelectionVersion = constraintSelectionVersion,
        identifier = this.identifier,
        project = this.project,
        task = this.task,
        history = this.history.toMutableList().also { it.add(constraintSelectionVersion) })
  }

  private fun TaskActionSelectionAggregateAvro.newTaskConstraintSelectionVersion():
      TaskConstraintSelectionVersion {
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

    return TaskConstraintSelectionVersion(
        version = this.aggregateIdentifier.version,
        constraints = this.actions.map { TaskConstraintEnum.valueOf(it.name) },
        eventAuthor = auditUser,
        eventDate = auditDate)
  }
}
