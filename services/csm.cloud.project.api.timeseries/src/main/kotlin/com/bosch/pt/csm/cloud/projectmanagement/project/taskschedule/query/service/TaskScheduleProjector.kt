/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.service

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.domain.asDayCardId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.asTaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.repository.TaskRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.domain.asTaskScheduleId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.model.TaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.model.TaskScheduleMapper
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.model.TaskScheduleSlot
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.model.TaskScheduleVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.repository.TaskScheduleRepository
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class TaskScheduleProjector(
    private val scheduleRepository: TaskScheduleRepository,
    private val taskRepository: TaskRepository
) {

  fun onTaskScheduleEvent(aggregate: TaskScheduleAggregateAvro, project: ProjectId) {
    val existingSchedule =
        scheduleRepository.findOneByIdentifier(aggregate.getIdentifier().asTaskScheduleId())

    if (existingSchedule == null || aggregate.getVersion() > existingSchedule.version) {
      (existingSchedule?.updateFromTaskScheduleAggregate(aggregate)
              ?: aggregate.toNewProjection(project))
          .apply { scheduleRepository.save(this) }
    }
  }

  fun onTaskScheduleDeletedEvent(aggregate: TaskScheduleAggregateAvro) {
    val schedule =
        scheduleRepository.findOneByIdentifier(aggregate.getIdentifier().asTaskScheduleId())
    if (schedule != null && !schedule.deleted) {
      val newVersion =
          schedule.history
              .last()
              .copy(
                  deleted = true,
                  version = aggregate.getVersion(),
                  eventDate =
                      aggregate.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis(),
                  eventAuthor =
                      aggregate.auditingInformation.lastModifiedBy.identifier.toUUID().asUserId())

      scheduleRepository.save(
          TaskScheduleMapper.INSTANCE.fromTaskScheduleVersion(
              newVersion,
              schedule.identifier,
              schedule.project,
              schedule.task,
              schedule.history.toMutableList().also { it.add(newVersion) }))
    }
  }

  private fun TaskScheduleAggregateAvro.toNewProjection(project: ProjectId): TaskSchedule {
    val taskScheduleVersion = this.newTaskScheduleVersion()

    return TaskScheduleMapper.INSTANCE.fromTaskScheduleVersion(
        taskScheduleVersion = taskScheduleVersion,
        identifier = aggregateIdentifier.identifier.toUUID().asTaskScheduleId(),
        project = project,
        task = task.identifier.toUUID().asTaskId(),
        history = listOf(taskScheduleVersion))
  }

  private fun TaskSchedule.updateFromTaskScheduleAggregate(
      aggregate: TaskScheduleAggregateAvro
  ): TaskSchedule {
    val taskScheduleVersion = aggregate.newTaskScheduleVersion()

    return TaskScheduleMapper.INSTANCE.fromTaskScheduleVersion(
        taskScheduleVersion = taskScheduleVersion,
        identifier = this.identifier,
        project = this.project,
        task = this.task,
        history = this.history.toMutableList().also { it.add(taskScheduleVersion) })
  }

  private fun TaskScheduleAggregateAvro.newTaskScheduleVersion(): TaskScheduleVersion {
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

    val task =
        taskRepository.findOneByIdentifier(this.task.identifier.toUUID().asTaskId())
            ?: error(
                "Could not find task ${this.task.identifier} of schedule ${this.aggregateIdentifier.identifier}")

    return TaskScheduleVersion(
        version = this.aggregateIdentifier.version,
        taskVersion = task.version,
        start = this.start?.toLocalDateByMillis(),
        end = this.end?.toLocalDateByMillis(),
        slots = readSlots(this),
        eventAuthor = auditUser,
        eventDate = auditDate)
  }

  private fun readSlots(schedule: TaskScheduleAggregateAvro) =
      schedule.slots.map {
        TaskScheduleSlot(
            it.date.toLocalDateByMillis(),
            it.dayCard.identifier.toUUID().asDayCardId(),
            it.dayCard.version)
      }
}
