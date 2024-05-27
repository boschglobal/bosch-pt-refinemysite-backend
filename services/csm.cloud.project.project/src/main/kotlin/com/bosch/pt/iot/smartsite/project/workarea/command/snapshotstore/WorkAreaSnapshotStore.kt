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
import com.bosch.pt.csm.cloud.common.messages.getCreatedByIdentifier
import com.bosch.pt.csm.cloud.common.messages.getLastModifiedByIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.WORKAREA
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORK_AREA_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextSnapshotStore
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import jakarta.persistence.EntityManager
import java.sql.Timestamp
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.slf4j.Logger
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class WorkAreaSnapshotStore(
    private val snapshotCache: WorkAreaSnapshotEntityCache,
    private val workAreaRepository: WorkAreaRepository,
    private val projectRepository: ProjectRepository,
    namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    em: EntityManager,
    logger: Logger
) :
    AbstractSnapshotStoreJdbc<WorkAreaEventAvro, WorkAreaSnapshot, WorkArea, WorkAreaId>(
        namedParameterJdbcTemplate, em, logger),
    ProjectContextSnapshotStore {

  override fun findOrFail(identifier: WorkAreaId) =
      snapshotCache.get(identifier)?.asSnapshot()
          ?: throw AggregateNotFoundException(
              WORK_AREA_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

  fun findOrIgnore(identifier: WorkAreaId) = snapshotCache.get(identifier)?.asSnapshot()

  override fun findInternal(identifier: UUID) = snapshotCache.get(identifier.asWorkAreaId())

  override fun isDeletedEvent(message: SpecificRecordBase) =
      (message as WorkAreaEventAvro).name == DELETED

  override fun updateInternal(
      event: WorkAreaEventAvro,
      currentSnapshot: WorkArea?,
      rootContextIdentifier: UUID
  ) {
    if (event.name == DELETED && currentSnapshot != null) {
      deleteWorkArea(currentSnapshot)
    } else {
      when (currentSnapshot == null) {
        true -> createWorkArea(event.aggregate, rootContextIdentifier.asProjectId())
        false -> {
          updateWorkArea(event.aggregate)
          removeFromPersistenceContext(currentSnapshot)
        }
      }
    }
  }

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase) =
      key.aggregateIdentifier.type == WORKAREA.value &&
          message is WorkAreaEventAvro &&
          message.name in setOf(CREATED, UPDATED, DELETED)

  private fun createWorkArea(aggregate: WorkAreaAggregateAvro, projectRef: ProjectId) {
    with(aggregate) {
      val projectId = findProjectIdOrFail(projectRef)

      MapSqlParameterSource()
          .addValue("identifier", aggregateIdentifier.identifier)
          .addValue("created_by", auditingInformation.getCreatedByIdentifier().toString())
          .addValue("created_date", Timestamp(auditingInformation.createdDate))
          .addValue("version", aggregateIdentifier.version)
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .addValue("name", aggregate.name)
          .addValue("project_id", projectId)
          .addValue("parent", aggregate.parent)
          .apply { execute(INSERT_STATEMENT, this) }
    }
  }

  private fun updateWorkArea(aggregate: WorkAreaAggregateAvro) {
    with(aggregate) {
      MapSqlParameterSource()
          .addValue("identifier", aggregateIdentifier.identifier)
          .addValue("version", aggregateIdentifier.version)
          .addValue("parent", aggregate.parent)
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .addValue("name", aggregate.name)
          .apply { execute(UPDATE_STATEMENT, this) }
    }
  }

  private fun deleteWorkArea(workArea: WorkArea) {
    workAreaRepository.delete(workArea)
  }

  private fun findProjectIdOrFail(projectRef: ProjectId): Long =
      requireNotNull(projectRepository.findIdByIdentifier(projectRef)) {
        "Could not find Project for the given projectId $projectRef"
      }

  companion object {

    private const val INSERT_STATEMENT =
        "INSERT INTO work_area " +
            "(identifier, version, created_by, created_date, last_modified_by, last_modified_date, " +
            "name, project_id, parent) " +
            "VALUES (:identifier, :version, :created_by, :created_date, :last_modified_by, :last_modified_date, " +
            ":name, :project_id, :parent)"

    private const val UPDATE_STATEMENT =
        "UPDATE work_area " +
            "SET version=:version," +
            "last_modified_by=:last_modified_by," +
            "last_modified_date=:last_modified_date," +
            "name=:name, " +
            "parent=:parent " +
            "WHERE identifier=:identifier AND version=:version-1"
  }
}
