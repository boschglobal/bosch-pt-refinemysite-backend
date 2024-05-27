/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJdbc
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.messages.getCreatedByIdentifier
import com.bosch.pt.csm.cloud.common.messages.getLastModifiedByIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.DAYCARD
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.APPROVED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.CANCELLED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.COMPLETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.RESET
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import com.bosch.pt.iot.smartsite.common.i18n.Key.DAY_CARD_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import com.bosch.pt.iot.smartsite.project.daycard.shared.repository.DayCardRepository
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextSnapshotStore
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
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
class DayCardSnapshotStore(
    private val repository: DayCardRepository,
    private val cachedRepository: DayCardSnapshotEntityCache,
    private val taskScheduleRepository: TaskScheduleRepository,
    namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    entityManager: EntityManager,
    logger: Logger
) :
    AbstractSnapshotStoreJdbc<DayCardEventG2Avro, DayCardSnapshot, DayCard, DayCardId>(
        namedParameterJdbcTemplate, entityManager, logger),
    ProjectContextSnapshotStore {

  override fun findInternal(identifier: UUID) = cachedRepository.get(identifier.asDayCardId())

  override fun isDeletedEvent(message: SpecificRecordBase) =
      (message as DayCardEventG2Avro).name == DELETED

  override fun findOrFail(identifier: DayCardId): DayCardSnapshot =
      cachedRepository.get(identifier)?.asSnapshot()
          ?: throw AggregateNotFoundException(
              DAY_CARD_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase): Boolean =
      key.aggregateIdentifier.type == DAYCARD.name &&
          message is DayCardEventG2Avro &&
          message.name in setOf(CREATED, UPDATED, DELETED, APPROVED, CANCELLED, COMPLETED, RESET)

  override fun updateInternal(
      event: DayCardEventG2Avro,
      currentSnapshot: DayCard?,
      rootContextIdentifier: UUID
  ) {
    if (event.name == DELETED && currentSnapshot != null) {
      deleteDayCard(currentSnapshot)
      cachedRepository.remove(currentSnapshot.identifier)
    } else {
      when (currentSnapshot == null) {
        true -> createDayCard(event.aggregate)
        false -> {
          updateDayCard(event.aggregate)
          removeFromPersistenceContext(currentSnapshot)
          cachedRepository.remove(currentSnapshot.identifier)
        }
      }
    }
  }

  private fun deleteDayCard(dayCard: DayCard) = repository.delete(dayCard)

  private fun createDayCard(aggregate: DayCardAggregateG2Avro) {
    with(aggregate) {
      val taskScheduleId =
          taskScheduleRepository.findIdByTaskIdentifier(aggregate.task.identifier.asTaskId())
      MapSqlParameterSource()
          .addValue("identifier", aggregateIdentifier.identifier)
          .addValue("created_by", auditingInformation.getCreatedByIdentifier().toString())
          .addValue("created_date", Timestamp(auditingInformation.createdDate))
          .addValue("version", aggregateIdentifier.version)
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .addValue("manpower", manpower)
          .addValue("notes", notes)
          .addValue("reason", reason?.name)
          .addValue("task_schedule_id", taskScheduleId)
          .addValue("status", status.name)
          .addValue("title", title)
          .apply { execute(INSERT_STATEMENT, this) }
    }
  }

  private fun updateDayCard(aggregate: DayCardAggregateG2Avro) {
    with(aggregate) {
      val taskScheduleId =
          taskScheduleRepository.findIdByTaskIdentifier(aggregate.task.identifier.asTaskId())

      MapSqlParameterSource()
          .addValue("identifier", aggregateIdentifier.identifier)
          .addValue("version", aggregateIdentifier.version)
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .addValue("title", title)
          .addValue("manpower", manpower)
          .addValue("notes", notes)
          .addValue("task_schedule_id", taskScheduleId)
          .addValue("reason", reason?.name)
          .addValue("status", status.name)
          .apply { execute(UPDATE_STATEMENT, this) }
    }
  }

  companion object {

    private const val INSERT_STATEMENT =
        "INSERT INTO day_card " +
            "(identifier, created_by, created_date, version, last_modified_by, last_modified_date, title, manpower, " +
            " notes, task_schedule_id, reason, status) " +
            "VALUES (:identifier, :created_by, :created_date, :version, :last_modified_by, :last_modified_date, " +
            ":title, :manpower, :notes, :task_schedule_id, :reason, :status)"

    private const val UPDATE_STATEMENT =
        "UPDATE day_card " +
            "SET version=:version, " +
            "last_modified_by=:last_modified_by, " +
            "last_modified_date=:last_modified_date, " +
            "title=:title, " +
            "manpower=:manpower, " +
            "notes=:notes, " +
            "task_schedule_id=:task_schedule_id, " +
            "reason=:reason, " +
            "status=:status " +
            "WHERE identifier=:identifier AND version=:version-1"
  }
}
