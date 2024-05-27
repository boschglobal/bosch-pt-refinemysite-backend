/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.query.service

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.domain.asProjectCraftId
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.domain.asParticipantId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.asTaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.model.Task
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.model.TaskMapper
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.model.TaskStatusEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.model.TaskVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.repository.TaskRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.domain.asWorkAreaId
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class TaskProjector(private val repository: TaskRepository) {

  fun onTaskEvent(aggregate: TaskAggregateAvro) {
    val existingTask = repository.findOneByIdentifier(aggregate.getIdentifier().asTaskId())

    if (existingTask == null || aggregate.aggregateIdentifier.version > existingTask.version) {
      (existingTask?.updateFromTaskAggregate(aggregate) ?: aggregate.toNewProjection()).apply {
        repository.save(this)
      }
    }
  }

  fun onTaskDeletedEvent(aggregate: TaskAggregateAvro) {
    val task = repository.findOneByIdentifier(aggregate.getIdentifier().asTaskId())
    if (task != null && !task.deleted) {
      val newVersion =
          task.history
              .last()
              .copy(
                  deleted = true,
                  version = aggregate.aggregateIdentifier.version,
                  eventDate =
                      aggregate.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis(),
                  eventAuthor =
                      aggregate.auditingInformation.lastModifiedBy.identifier.toUUID().asUserId())

      repository.save(
          TaskMapper.INSTANCE.fromTaskVersion(
              newVersion,
              task.identifier,
              task.project,
              task.history.toMutableList().also { it.add(newVersion) }))
    }
  }

  private fun TaskAggregateAvro.toNewProjection(): Task {
    val taskVersion = this.newTaskVersion()

    return TaskMapper.INSTANCE.fromTaskVersion(
        taskVersion = taskVersion,
        identifier = aggregateIdentifier.identifier.toUUID().asTaskId(),
        project = project.identifier.toUUID().asProjectId(),
        history = listOf(taskVersion))
  }

  private fun Task.updateFromTaskAggregate(aggregate: TaskAggregateAvro): Task {
    val taskVersion = aggregate.newTaskVersion()

    return TaskMapper.INSTANCE.fromTaskVersion(
        taskVersion = taskVersion,
        identifier = this.identifier,
        project = this.project,
        history = this.history.toMutableList().also { it.add(taskVersion) })
  }

  private fun TaskAggregateAvro.newTaskVersion(): TaskVersion {
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

    return TaskVersion(
        version = this.aggregateIdentifier.version,
        name = this.name,
        description = this.description,
        location = this.location,
        craft = this.craft.identifier.toUUID().asProjectCraftId(),
        assignee = this.assignee?.identifier?.toUUID()?.asParticipantId(),
        status = TaskStatusEnum.valueOf(this.status.name),
        editDate = this.editDate?.toLocalDateTimeByMillis(),
        workArea = this.workarea?.identifier?.toUUID()?.asWorkAreaId(),
        eventAuthor = auditUser,
        eventDate = auditDate)
  }
}
