/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJdbc
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.getCreatedByIdentifier
import com.bosch.pt.csm.cloud.common.messages.getLastModifiedByIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.WORKAREALIST
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro.ITEMADDED
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro.REORDERED
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORK_AREA_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextSnapshotStore
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaListId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaListId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkAreaList
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaListRepository
import jakarta.persistence.EntityManager
import java.sql.Timestamp
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.slf4j.Logger
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class WorkAreaListSnapshotStore(
    private val snapshotCache: WorkAreaListSnapshotEntityCache,
    private val workAreaListRepository: WorkAreaListRepository,
    private val projectRepository: ProjectRepository,
    namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    em: EntityManager,
    logger: Logger
) :
    AbstractSnapshotStoreJdbc<
        WorkAreaListEventAvro, WorkAreaListSnapshot, WorkAreaList, WorkAreaListId>(
        namedParameterJdbcTemplate, em, logger),
    ProjectContextSnapshotStore {

  override fun findOrFail(identifier: WorkAreaListId) =
      snapshotCache.get(identifier)?.asSnapshot()
          ?: throw AggregateNotFoundException(
              WORK_AREA_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

  override fun findInternal(identifier: UUID) = snapshotCache.get(identifier.asWorkAreaListId())

  override fun isDeletedEvent(message: SpecificRecordBase): Boolean {
    // This function will always return false because there is no 'DELETED' event specified for the
    // 'WorkAreaList'.
    return false
  }

  override fun updateInternal(
      event: WorkAreaListEventAvro,
      currentSnapshot: WorkAreaList?,
      rootContextIdentifier: UUID
  ) {
    when (currentSnapshot == null) {
      true -> createWorkAreaList(event.aggregate)
      false -> {
        updateWorkAreaList(event.aggregate)
        removeFromPersistenceContext(currentSnapshot)
        snapshotCache.remove(currentSnapshot.identifier)
      }
    }
  }

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase) =
      key.aggregateIdentifier.type == WORKAREALIST.value &&
          message is WorkAreaListEventAvro &&
          message.name in setOf(CREATED, ITEMADDED, REORDERED)

  private fun createWorkAreaList(aggregate: WorkAreaListAggregateAvro) {
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
          .apply { execute(INSERT_STATEMENT, this) }

      updateWorkAreaPositions(aggregate)
    }
  }

  private fun updateWorkAreaList(aggregate: WorkAreaListAggregateAvro) {
    with(aggregate) {
      MapSqlParameterSource()
          .addValue("identifier", aggregateIdentifier.identifier)
          .addValue("version", aggregateIdentifier.version)
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .apply { execute(UPDATE_STATEMENT, this) }

      updateWorkAreaPositions(aggregate)
    }
  }

  private fun updateWorkAreaPositions(aggregate: WorkAreaListAggregateAvro) {
    val workAreaListId = findWorkAreaListIdOrFail(aggregate.aggregateIdentifier)
    with(aggregate) {
      workAreas.forEachIndexed { index, workArea ->
        MapSqlParameterSource()
            .addValue("identifier", workArea.identifier)
            .addValue("position", index)
            .addValue("work_area_list_id", workAreaListId)
            .apply { execute(UPDATE_WORK_AREA_POSITION_STATEMENT, this) }
      }
    }
  }

  private fun findProjectIdOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): Long =
      requireNotNull(
          projectRepository.findIdByIdentifier(aggregateIdentifierAvro.identifier.asProjectId())) {
            "Could not find Project ${aggregateIdentifierAvro.identifier}"
          }

  private fun findWorkAreaListIdOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): Long =
      requireNotNull(
          workAreaListRepository.findIdByIdentifier(
              aggregateIdentifierAvro.identifier.asWorkAreaListId())) {
            "Could not find WorkAreaList ${aggregateIdentifierAvro.identifier}"
          }

  companion object {

    private const val INSERT_STATEMENT =
        "INSERT INTO work_area_list " +
            "(identifier, version, created_by, created_date, last_modified_by, last_modified_date, " +
            "project_id) " +
            "VALUES (:identifier, :version, :created_by, :created_date, :last_modified_by, :last_modified_date, " +
            ":project_id)"

    private const val UPDATE_STATEMENT =
        "UPDATE work_area_list " +
            "SET version=:version, " +
            "last_modified_by=:last_modified_by, " +
            "last_modified_date=:last_modified_date " +
            "WHERE identifier=:identifier AND version=:version-1"

    private const val UPDATE_WORK_AREA_POSITION_STATEMENT =
        "UPDATE work_area " +
            "SET position=:position," +
            "work_area_list_id=:work_area_list_id " +
            "WHERE identifier=:identifier"
  }
}
