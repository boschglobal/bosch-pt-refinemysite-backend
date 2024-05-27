/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJdbc
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.getCreatedByIdentifier
import com.bosch.pt.csm.cloud.common.messages.getLastModifiedByIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECTCRAFT
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventG2Avro
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_CRAFT_VALIDATION_ERROR_PROJECT_CRAFT_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextSnapshotStore
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftRepository
import java.sql.Timestamp
import java.util.UUID
import jakarta.persistence.EntityManager
import org.apache.avro.specific.SpecificRecordBase
import org.slf4j.Logger
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class ProjectCraftSnapshotStore(
    private val repository: ProjectCraftRepository,
    private val cachedRepository: ProjectCraftSnapshotEntityCache,
    private val projectRepository: ProjectRepository,
    namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    entityManager: EntityManager,
    logger: Logger
) :
    AbstractSnapshotStoreJdbc<
        ProjectCraftEventG2Avro, ProjectCraftSnapshot, ProjectCraft, ProjectCraftId>(
        namedParameterJdbcTemplate, entityManager, logger),
    ProjectContextSnapshotStore {

  override fun findOrFail(identifier: ProjectCraftId): ProjectCraftSnapshot =
      cachedRepository.get(identifier)?.asSnapshot()
          ?: throw AggregateNotFoundException(
              PROJECT_CRAFT_VALIDATION_ERROR_PROJECT_CRAFT_NOT_FOUND, identifier.toString())

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase): Boolean =
      key.aggregateIdentifier.type == PROJECTCRAFT.name &&
          message is ProjectCraftEventG2Avro &&
          message.name in setOf(CREATED, UPDATED, DELETED)

  override fun isDeletedEvent(message: SpecificRecordBase) =
      (message as ProjectCraftEventG2Avro).name == DELETED

  override fun updateInternal(
      event: ProjectCraftEventG2Avro,
      currentSnapshot: ProjectCraft?,
      rootContextIdentifier: UUID
  ) {
    if (event.name == DELETED && currentSnapshot != null) {
      deleteProjectCraft(currentSnapshot)
      cachedRepository.remove(currentSnapshot.identifier)
    } else {
      when (currentSnapshot == null) {
        true -> createProjectCraft(event.aggregate)
        false -> {
          updateProjectCraft(event.aggregate)
          removeFromPersistenceContext(currentSnapshot)
          cachedRepository.remove(currentSnapshot.identifier)
        }
      }
    }
  }

  override fun findInternal(identifier: UUID) = cachedRepository.get(identifier.asProjectCraftId())

  private fun findProjectIdOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): Long =
      requireNotNull(
          projectRepository.findIdByIdentifier(aggregateIdentifierAvro.identifier.asProjectId())) {
            "Could not find ProjectCraft ${aggregateIdentifierAvro.identifier}"
          }

  private fun createProjectCraft(aggregate: ProjectCraftAggregateG2Avro) {
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
          .addValue("name", name)
          .addValue("color", color)
          .apply { execute(INSERT_STATEMENT, this) }
    }
  }

  private fun updateProjectCraft(aggregate: ProjectCraftAggregateG2Avro) {
    with(aggregate) {
      MapSqlParameterSource()
          .addValue("identifier", aggregateIdentifier.identifier)
          .addValue("version", aggregateIdentifier.version)
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .addValue("name", name)
          .addValue("color", color)
          .apply { execute(UPDATE_STATEMENT, this) }
    }
  }

  private fun deleteProjectCraft(projectCraft: ProjectCraft) = repository.delete(projectCraft)

  companion object {

    private const val INSERT_STATEMENT =
        "INSERT INTO project_craft " +
            "(identifier, created_by, created_date, version, last_modified_by, last_modified_date, project_id, name, " +
            " color) " +
            "VALUES (:identifier, :created_by, :created_date, :version, :last_modified_by, :last_modified_date, " +
            ":project_id, :name, :color)"

    private const val UPDATE_STATEMENT =
        "UPDATE project_craft " +
            "SET version=:version," +
            "last_modified_by=:last_modified_by," +
            "last_modified_date=:last_modified_date," +
            "name=:name," +
            "color=:color " +
            "WHERE identifier=:identifier AND version=:version-1"
  }
}
