/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.command.snapshotshore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJdbc
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.getCreatedByIdentifier
import com.bosch.pt.csm.cloud.common.messages.getLastModifiedByIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.WORKDAYCONFIGURATION
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextSnapshotStore
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.workday.domain.WorkdayConfigurationId
import com.bosch.pt.iot.smartsite.project.workday.domain.asWorkdayConfigurationId
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import com.bosch.pt.iot.smartsite.project.workday.shared.repository.WorkdayConfigurationRepository
import java.sql.Timestamp
import java.util.UUID
import jakarta.persistence.EntityManager
import org.apache.avro.specific.SpecificRecordBase
import org.slf4j.Logger
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class WorkdayConfigurationSnapshotStore(
    private val repository: WorkdayConfigurationRepository,
    private val cachedRepository: WorkdayConfigurationSnapshotEntityCache,
    private val projectRepository: ProjectRepository,
    namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    entityManager: EntityManager,
    logger: Logger
) :
    AbstractSnapshotStoreJdbc<
        WorkdayConfigurationEventAvro,
        WorkdayConfigurationSnapshot,
        WorkdayConfiguration,
        WorkdayConfigurationId>(namedParameterJdbcTemplate, entityManager, logger),
    ProjectContextSnapshotStore {

  override fun findOrFail(identifier: WorkdayConfigurationId): WorkdayConfigurationSnapshot =
      requireNotNull(cachedRepository.get(identifier)?.asSnapshot())

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase): Boolean =
      key.aggregateIdentifier.type == WORKDAYCONFIGURATION.name &&
          message is WorkdayConfigurationEventAvro &&
          message.name in setOf(CREATED, UPDATED, DELETED)

  override fun isDeletedEvent(message: SpecificRecordBase) =
      (message as WorkdayConfigurationEventAvro).name == DELETED

  override fun updateInternal(
      event: WorkdayConfigurationEventAvro,
      currentSnapshot: WorkdayConfiguration?,
      rootContextIdentifier: UUID
  ) {
    if (event.name == DELETED && currentSnapshot != null) {
      delete(currentSnapshot)
      cachedRepository.remove(currentSnapshot.identifier)
    } else {
      when (currentSnapshot == null) {
        true -> create(event.aggregate)
        false -> {
          update(event.aggregate)
          removeFromPersistenceContext(currentSnapshot)
          cachedRepository.remove(currentSnapshot.identifier)
        }
      }
    }
  }

  override fun findInternal(identifier: UUID) =
      cachedRepository.get(identifier.asWorkdayConfigurationId())

  private fun findProjectIdOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): Long =
      requireNotNull(
          projectRepository.findIdByIdentifier(aggregateIdentifierAvro.identifier.asProjectId())) {
            "Could not find the project ${aggregateIdentifierAvro.identifier}"
          }

  private fun create(aggregate: WorkdayConfigurationAggregateAvro) =
      with(aggregate) {
        val projectId = findProjectIdOrFail(project)

        MapSqlParameterSource()
            .addValue("identifier", aggregateIdentifier.identifier)
            .addValue("created_by", auditingInformation.getCreatedByIdentifier().toString())
            .addValue("created_date", Timestamp(auditingInformation.createdDate))
            .addValue("version", aggregateIdentifier.version)
            .addValue(
                "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
            .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
            .addValue("project_id", projectId)
            .addValue("start_of_week", startOfWeek.name)
            .addValue("allow_work_on_non_working_days", allowWorkOnNonWorkingDays)
            .apply { execute(INSERT_STATEMENT, this) }

        insertWorkingDays(aggregate, projectId)
        insertHolidays(aggregate, projectId)
      }

  private fun update(aggregate: WorkdayConfigurationAggregateAvro) =
      with(aggregate) {
        val projectId = findProjectIdOrFail(project)
        val existingWorkdayConfiguration =
            requireNotNull(
                repository.findOneWithDetailsByIdentifier(
                    aggregateIdentifier.identifier.asWorkdayConfigurationId())) {
                  "Could not find the workday configuration ${aggregateIdentifier.identifier}"
                }

        MapSqlParameterSource()
            .addValue("identifier", aggregateIdentifier.identifier)
            .addValue("version", aggregateIdentifier.version)
            .addValue(
                "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
            .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
            .addValue("start_of_week", startOfWeek.name)
            .addValue("allow_work_on_non_working_days", allowWorkOnNonWorkingDays)
            .apply { execute(UPDATE_STATEMENT, this) }

        if (hasWorkingDaysChanged(existingWorkdayConfiguration, aggregate)) {
          deleteWorkingDays(existingWorkdayConfiguration, projectId)
          insertWorkingDays(aggregate, projectId)
        }

        if (hasHolidaysChanged(existingWorkdayConfiguration, aggregate)) {
          deleteHolidays(existingWorkdayConfiguration, projectId)
          insertHolidays(aggregate, projectId)
        }
      }

  private fun delete(entity: WorkdayConfiguration) = repository.delete(entity)

  private fun hasWorkingDaysChanged(
      existingWorkdayConfiguration: WorkdayConfiguration,
      aggregate: WorkdayConfigurationAggregateAvro
  ) =
      existingWorkdayConfiguration.workingDays.map { it.name }.toSet() !=
          aggregate.workingDays.map { it.name }.toSet()

  private fun insertWorkingDays(aggregate: WorkdayConfigurationAggregateAvro, projectId: Long) =
      with(aggregate) {
        for (workingDay in workingDays) {
          MapSqlParameterSource()
              .addValue("workday_configuration_project_id", projectId)
              .addValue("working_days", workingDay.name)
              .apply { execute(INSERT_WORKING_DAYS_STATEMENT, this) }
        }
      }

  private fun deleteWorkingDays(
      existingWorkdayConfiguration: WorkdayConfiguration,
      projectId: Long
  ) =
      with(existingWorkdayConfiguration) {
        for (workingDay in workingDays) {
          MapSqlParameterSource()
              .addValue("workday_configuration_project_id", projectId)
              .addValue("working_days", workingDay.name)
              .apply { execute(DELETE_WORKING_DAYS_STATEMENT, this) }
        }
      }

  private fun hasHolidaysChanged(
      existingWorkdayConfiguration: WorkdayConfiguration,
      aggregate: WorkdayConfigurationAggregateAvro
  ) = existingWorkdayConfiguration.holidays != aggregate.holidays

  private fun insertHolidays(aggregate: WorkdayConfigurationAggregateAvro, projectId: Long) =
      with(aggregate) {
        for (holiday in holidays) {
          MapSqlParameterSource()
              .addValue("workday_configuration_project_id", projectId)
              .addValue("name", holiday.name)
              .addValue("date", Timestamp(holiday.date))
              .apply { execute(INSERT_HOLIDAYS_STATEMENT, this) }
        }
      }

  private fun deleteHolidays(existingWorkdayConfiguration: WorkdayConfiguration, projectId: Long) =
      with(existingWorkdayConfiguration) {
        for (holiday in holidays) {
          MapSqlParameterSource()
              .addValue("workday_configuration_project_id", projectId)
              .addValue("name", holiday.name)
              .addValue("date", holiday.date)
              .apply { execute(DELETE_HOLIDAYS_STATEMENT, this) }
        }
      }

  companion object {
    private const val INSERT_STATEMENT =
        "INSERT INTO workday_configuration (identifier, created_by, created_date, version, last_modified_by, " +
            "last_modified_date, project_id, start_of_week, allow_work_on_non_working_days) " +
            "VALUES (:identifier, :created_by, :created_date, :version, :last_modified_by, :last_modified_date, " +
            ":project_id, :start_of_week, :allow_work_on_non_working_days)"

    private const val UPDATE_STATEMENT =
        "UPDATE workday_configuration SET version=:version, last_modified_by=:last_modified_by, " +
            "last_modified_date=:last_modified_date, start_of_week=:start_of_week, " +
            "allow_work_on_non_working_days=:allow_work_on_non_working_days " +
            "WHERE identifier=:identifier AND version=:version-1"

    private const val INSERT_WORKING_DAYS_STATEMENT =
        "INSERT INTO workday_configuration_working_days (workday_configuration_project_id, working_days) " +
            "VALUES (:workday_configuration_project_id, :working_days)"

    private const val DELETE_WORKING_DAYS_STATEMENT =
        "DELETE FROM workday_configuration_working_days " +
            "WHERE workday_configuration_project_id=:workday_configuration_project_id AND working_days=:working_days"

    private const val INSERT_HOLIDAYS_STATEMENT =
        "INSERT INTO workday_configuration_holidays (workday_configuration_project_id, name, date) " +
            "VALUES (:workday_configuration_project_id, :name, :date)"

    private const val DELETE_HOLIDAYS_STATEMENT =
        "DELETE FROM workday_configuration_holidays " +
            "WHERE workday_configuration_project_id=:workday_configuration_project_id AND name=:name AND date=:date"
  }
}
