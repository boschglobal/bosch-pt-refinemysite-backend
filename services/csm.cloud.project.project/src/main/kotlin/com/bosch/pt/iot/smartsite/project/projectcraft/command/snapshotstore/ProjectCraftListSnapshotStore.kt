/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJdbc
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.getCreatedByIdentifier
import com.bosch.pt.csm.cloud.common.messages.getLastModifiedByIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECTCRAFTLIST
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro.ITEMADDED
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro.ITEMREMOVED
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro.REORDERED
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextSnapshotStore
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftListId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftListId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraftList
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftListRepository
import jakarta.persistence.EntityManager
import java.sql.Timestamp
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.slf4j.Logger
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class ProjectCraftListSnapshotStore(
    private val cachedRepository: ProjectCraftListSnapshotEntityCache,
    private val projectRepository: ProjectRepository,
    private val projectCraftListRepository: ProjectCraftListRepository,
    namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    em: EntityManager,
    logger: Logger
) :
    AbstractSnapshotStoreJdbc<
        ProjectCraftListEventAvro, ProjectCraftListSnapshot, ProjectCraftList, ProjectCraftListId>(
        namedParameterJdbcTemplate, em, logger),
    ProjectContextSnapshotStore {

  override fun findOrFail(identifier: ProjectCraftListId): ProjectCraftListSnapshot =
      requireNotNull(cachedRepository.get(identifier)?.asSnapshot())

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase): Boolean {
    return key.aggregateIdentifier.type == PROJECTCRAFTLIST.value &&
        message is ProjectCraftListEventAvro &&
        message.name in setOf(CREATED, ITEMADDED, ITEMREMOVED, REORDERED)
  }

  override fun isDeletedEvent(message: SpecificRecordBase): Boolean {
    // This function will always return false because there is no 'DELETED' event specified for the
    // 'ProjectCraftList'.
    return false
  }

  override fun updateInternal(
      event: ProjectCraftListEventAvro,
      currentSnapshot: ProjectCraftList?,
      rootContextIdentifier: UUID
  ) {
    when (currentSnapshot == null) {
      true -> createProjectCraftList(event.aggregate)
      false -> {
        updateProjectCraftList(event.aggregate)
        removeFromPersistenceContext(currentSnapshot)
        cachedRepository.remove(currentSnapshot.identifier)
      }
    }
  }

  override fun findInternal(identifier: UUID): ProjectCraftList? =
      cachedRepository.get(identifier.asProjectCraftListId())

  private fun findProjectIdOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): Long =
      requireNotNull(
          projectRepository.findIdByIdentifier(aggregateIdentifierAvro.identifier.asProjectId())) {
            "Could not find Project ${aggregateIdentifierAvro.identifier}"
          }

  private fun findProjectCraftListIdOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): Long =
      requireNotNull(
          projectCraftListRepository.findIdByIdentifier(
              aggregateIdentifierAvro.identifier.asProjectCraftListId())) {
            "Could not find ProjectCraftList ${aggregateIdentifierAvro.identifier}"
          }

  private fun createProjectCraftList(aggregate: ProjectCraftListAggregateAvro) {
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

      updateProjectCraftPositions(this)
    }
  }

  private fun updateProjectCraftList(aggregate: ProjectCraftListAggregateAvro) {
    with(aggregate) {
      MapSqlParameterSource()
          .addValue("identifier", aggregateIdentifier.identifier)
          .addValue("version", aggregateIdentifier.version)
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .apply { execute(UPDATE_STATEMENT, this) }

      updateProjectCraftPositions(this)
    }
  }

  private fun updateProjectCraftPositions(aggregate: ProjectCraftListAggregateAvro) {
    val projectCraftListId = findProjectCraftListIdOrFail(aggregate.aggregateIdentifier)

    with(aggregate) {
      projectCrafts.forEachIndexed { index, projectCraft ->
        MapSqlParameterSource()
            .addValue("identifier", projectCraft.identifier)
            .addValue("position", index)
            .addValue("project_craft_list_id", projectCraftListId)
            .apply { execute(UPDATE_PROJECT_CRAFT_POSITION_STATEMENT, this) }
      }
    }
  }

  companion object {
    private const val INSERT_STATEMENT =
        "INSERT INTO project_craft_list (identifier, created_by, created_date, version, last_modified_by, " +
            "last_modified_date, project_id) " +
            "VALUES (:identifier, :created_by, :created_date, :version, :last_modified_by, :last_modified_date, " +
            ":project_id)"

    private const val UPDATE_STATEMENT =
        "UPDATE project_craft_list " +
            "SET version=:version, " +
            "last_modified_by=:last_modified_by, " +
            "last_modified_date=:last_modified_date " +
            "WHERE identifier=:identifier AND version=:version-1"

    private const val UPDATE_PROJECT_CRAFT_POSITION_STATEMENT =
        "UPDATE project_craft " +
            "SET position=:position," +
            "project_craft_list_id=:project_craft_list_id " +
            "WHERE identifier=:identifier"
  }
}
