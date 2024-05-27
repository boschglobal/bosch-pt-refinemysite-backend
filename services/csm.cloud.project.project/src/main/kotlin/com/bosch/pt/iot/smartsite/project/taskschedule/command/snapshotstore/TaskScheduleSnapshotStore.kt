/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJdbc
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.messages.getCreatedByIdentifier
import com.bosch.pt.csm.cloud.common.messages.getLastModifiedByIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASKSCHEDULE
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.repository.DayCardRepository
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextSnapshotStore
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.asTaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository.TaskScheduleRepository
import jakarta.persistence.EntityManager
import java.sql.Timestamp
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.slf4j.Logger
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class TaskScheduleSnapshotStore(
    private val repository: TaskScheduleRepository,
    private val snapshotCache: TaskScheduleSnapshotEntityCache,
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val taskScheduleRepository: TaskScheduleRepository,
    private val dayCardRepository: DayCardRepository,
    namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    entityManager: EntityManager,
    logger: Logger
) :
    AbstractSnapshotStoreJdbc<
        TaskScheduleEventAvro, TaskScheduleSnapshot, TaskSchedule, TaskScheduleId>(
        namedParameterJdbcTemplate, entityManager, logger),
    ProjectContextSnapshotStore {

  override fun findOrFail(identifier: TaskScheduleId): TaskScheduleSnapshot =
      snapshotCache.get(identifier)?.asSnapshot()
          ?: throw AggregateNotFoundException(
              TASK_SCHEDULE_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

  override fun findInternal(identifier: UUID) = snapshotCache.get(identifier.asTaskScheduleId())

  override fun isDeletedEvent(message: SpecificRecordBase) =
      (message as TaskScheduleEventAvro).name == DELETED

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase): Boolean =
      key.aggregateIdentifier.type == TASKSCHEDULE.name &&
          message is TaskScheduleEventAvro &&
          message.name in setOf(CREATED, UPDATED, DELETED)

  override fun updateInternal(
      event: TaskScheduleEventAvro,
      currentSnapshot: TaskSchedule?,
      rootContextIdentifier: UUID
  ) {
    if (event.name == DELETED && currentSnapshot != null) {
      deleteTaskSchedule(currentSnapshot)
      snapshotCache.remove(currentSnapshot.identifier)
    } else {
      when (currentSnapshot == null) {
        true -> createTaskSchedule(event.aggregate)
        false -> {
          updateTaskSchedule(event.aggregate)
          removeFromPersistenceContext(currentSnapshot)
          snapshotCache.remove(currentSnapshot.identifier)
        }
      }
    }
  }

  private fun deleteTaskSchedule(taskSchedule: TaskSchedule) = repository.delete(taskSchedule)

  private fun createTaskSchedule(aggregate: TaskScheduleAggregateAvro) {
    with(aggregate) {
      val taskList = taskRepository.findAllByIdentifierIn(listOf(task.identifier.asTaskId()))
      val projectId = projectRepository.findIdByIdentifier(taskList.first().project.identifier)
      val taskId = taskRepository.findIdByIdentifier(taskList.first().identifier)

      aggregateIdentifier
      MapSqlParameterSource()
          .addValue("identifier", aggregateIdentifier.identifier)
          .addValue("created_by", auditingInformation.getCreatedByIdentifier().toString())
          .addValue("created_date", Timestamp(auditingInformation.createdDate))
          .addValue("version", aggregateIdentifier.version)
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .addValue("start_date", start?.let { Timestamp(start) })
          .addValue("end_date", end?.let { Timestamp(end) })
          .addValue("project_id", projectId)
          .addValue("task_id", taskId)
          .apply { execute(INSERT_STATEMENT, this) }

      insertSlots(aggregate)
    }
  }

  private fun updateTaskSchedule(aggregate: TaskScheduleAggregateAvro) {
    val existingTaskSchedule =
        taskScheduleRepository.findOneByTaskIdentifier(aggregate.task.identifier.asTaskId())!!
    with(aggregate) {
      MapSqlParameterSource()
          .addValue("identifier", aggregateIdentifier.identifier)
          .addValue("version", aggregateIdentifier.version)
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .addValue("start_date", start?.let { Timestamp(start) })
          .addValue("end_date", end?.let { Timestamp(end) })
          .apply { execute(UPDATE_STATEMENT, this) }

      if (hasSlotsChanged(existingTaskSchedule, aggregate)) {
        deleteSlots(existingTaskSchedule)
        insertSlots(aggregate)
      }
    }
  }

  private fun hasSlotsChanged(
      existingTaskSchedule: TaskSchedule,
      aggregate: TaskScheduleAggregateAvro
  ) = existingTaskSchedule.slots != aggregate.slots

  private fun insertSlots(aggregate: TaskScheduleAggregateAvro) {
    val taskScheduleIdentifier =
        taskScheduleRepository.findIdByTaskIdentifier(aggregate.task.identifier.asTaskId())

    with(aggregate) {
      val dayCards =
          dayCardRepository.findAllEntitiesWithDetailsByIdentifierIn(
              slots.map { it.dayCard.identifier.asDayCardId() }.toSet())

      for (slot in slots) {
        val dayCardId = dayCards.find { it.identifier.toString() == slot.dayCard.identifier }?.id
        if (dayCardId != null) {
          MapSqlParameterSource()
              .addValue("taskschedule_id", taskScheduleIdentifier)
              .addValue("day_card_date", Timestamp(slot.date))
              .addValue("day_card_id", dayCardId)
              .apply { execute(INSERT_SLOTS_STATEMENT, this) }
        }
      }
    }
  }

  private fun deleteSlots(existingTaskSchedule: TaskSchedule) {
    val taskScheduleIdentifier =
        taskScheduleRepository.findIdByTaskIdentifier(existingTaskSchedule.task.identifier)
    with(existingTaskSchedule) {
      if (slots != null) {
        for (slot in slots!!) {
          MapSqlParameterSource()
              .addValue("taskschedule_id", taskScheduleIdentifier)
              .addValue("day_card_date", slot.date)
              .addValue("day_card_id", slot.dayCard.id)
              .apply { execute(DELETE_SLOTS_STATEMENT, this) }
        }
      }
    }
  }

  companion object {

    private const val INSERT_STATEMENT =
        "INSERT INTO task_schedule " +
            "(identifier, created_by, created_date, version, last_modified_by, last_modified_date, " +
            "start_date, end_date, project_id, task_id) " +
            "VALUES (:identifier, :created_by, :created_date, :version, :last_modified_by, :last_modified_date, " +
            ":start_date, :end_date, :project_id, :task_id)"

    private const val UPDATE_STATEMENT =
        "UPDATE task_schedule " +
            "SET version=:version, " +
            "last_modified_by=:last_modified_by, " +
            "last_modified_date=:last_modified_date, " +
            "start_date=:start_date, " +
            "end_date=:end_date " +
            "WHERE identifier=:identifier AND version=:version-1"

    private const val INSERT_SLOTS_STATEMENT =
        "INSERT INTO taskschedule_taskscheduleslot (taskschedule_id, day_card_date, day_card_id) " +
            "VALUES (:taskschedule_id, :day_card_date, :day_card_id)"

    private const val DELETE_SLOTS_STATEMENT =
        "DELETE FROM taskschedule_taskscheduleslot " +
            "WHERE taskschedule_id=:taskschedule_id " +
            "AND day_card_date=:day_card_date " +
            "AND day_card_id=:day_card_id"
  }
}
