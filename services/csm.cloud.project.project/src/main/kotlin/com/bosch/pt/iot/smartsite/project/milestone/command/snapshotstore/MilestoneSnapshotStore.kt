/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJdbc
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.getCreatedByIdentifier
import com.bosch.pt.csm.cloud.common.messages.getLastModifiedByIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.MILESTONE
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.i18n.Key.MILESTONE_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextSnapshotStore
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneRepository
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftRepository
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import java.sql.Timestamp
import java.util.UUID
import jakarta.persistence.EntityManager
import org.apache.avro.specific.SpecificRecordBase
import org.slf4j.Logger
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class MilestoneSnapshotStore(
    private val snapshotCache: MilestoneSnapshotEntityCache,
    private val milestoneRepository: MilestoneRepository,
    private val projectRepository: ProjectRepository,
    private val projectCraftRepository: ProjectCraftRepository,
    private val workAreRepository: WorkAreaRepository,
    namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    em: EntityManager,
    logger: Logger
) :
    AbstractSnapshotStoreJdbc<MilestoneEventAvro, MilestoneSnapshot, Milestone, MilestoneId>(
        namedParameterJdbcTemplate, em, logger),
    ProjectContextSnapshotStore {

  override fun findOrFail(identifier: MilestoneId) =
      snapshotCache.get(identifier)?.asSnapshot()
          ?: throw AggregateNotFoundException(
              MILESTONE_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

  fun findAllOrIgnore(identifiers: List<MilestoneId>) =
      snapshotCache.populateFromCall { snapshotCache.loadAllFromDatabase(identifiers) }

  fun findOrIgnore(identifier: MilestoneId) = snapshotCache.get(identifier)?.asSnapshot()

  override fun findInternal(identifier: UUID) = snapshotCache.get(identifier.asMilestoneId())

  override fun isDeletedEvent(message: SpecificRecordBase) =
      (message as MilestoneEventAvro).name == DELETED

  override fun updateInternal(
      event: MilestoneEventAvro,
      currentSnapshot: Milestone?,
      rootContextIdentifier: UUID
  ) {
    if (event.name == DELETED && currentSnapshot != null) {
      deleteMilestone(currentSnapshot)
      snapshotCache.remove(currentSnapshot.identifier)
    } else {
      when (currentSnapshot == null) {
        true -> createMilestone(event.aggregate)
        false -> {
          updateMilestone(event.aggregate)
          removeFromPersistenceContext(currentSnapshot)
          snapshotCache.remove(currentSnapshot.identifier)
        }
      }
    }
  }

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase) =
      key.aggregateIdentifier.type == MILESTONE.value &&
          message is MilestoneEventAvro &&
          message.name in setOf(CREATED, UPDATED, DELETED)

  private fun createMilestone(aggregate: MilestoneAggregateAvro) {
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
          .addValue("name", aggregate.name)
          .addValue("type", aggregate.type.ordinal)
          .addValue("date", Timestamp(aggregate.date))
          .addValue("header", aggregate.header)
          .addValue("project_id", projectId)
          .addValue("craft_id", aggregate.craft?.let { findProjectCraftIdOrFail(aggregate.craft) })
          .addValue(
              "work_area_id", aggregate.workarea?.let { findWorkAreaIdOrFail(aggregate.workarea) })
          .addValue("description", aggregate.description)
          .apply { execute(INSERT_STATEMENT, this) }
    }
  }

  private fun updateMilestone(aggregate: MilestoneAggregateAvro) {
    with(aggregate) {
      MapSqlParameterSource()
          .addValue("identifier", aggregateIdentifier.identifier)
          .addValue("version", aggregateIdentifier.version)
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .addValue("name", aggregate.name)
          .addValue("type", aggregate.type.ordinal)
          .addValue("date", Timestamp(aggregate.date))
          .addValue("header", aggregate.header)
          .addValue("craft_id", aggregate.craft?.let { findProjectCraftIdOrFail(aggregate.craft) })
          .addValue(
              "work_area_id", aggregate.workarea?.let { findWorkAreaIdOrFail(aggregate.workarea) })
          .addValue("description", aggregate.description)
          .apply { execute(UPDATE_STATEMENT, this) }
    }
  }

  private fun deleteMilestone(milestone: Milestone) {
    milestoneRepository.delete(milestone)
  }

  private fun findProjectIdOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): Long =
      requireNotNull(
          projectRepository.findIdByIdentifier(aggregateIdentifierAvro.identifier.asProjectId())) {
            "Could not find Project ${aggregateIdentifierAvro.identifier}"
          }

  private fun findProjectCraftIdOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): Long =
      requireNotNull(
          projectCraftRepository.findIdByIdentifier(
              aggregateIdentifierAvro.identifier.asProjectCraftId())) {
            "Could not find ProjectCraft ${aggregateIdentifierAvro.identifier}"
          }

  private fun findWorkAreaIdOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): Long =
      requireNotNull(
          workAreRepository.findIdByIdentifier(aggregateIdentifierAvro.identifier.asWorkAreaId())) {
            "Could not find WorkArea ${aggregateIdentifierAvro.identifier}"
          }

  companion object {

    private const val INSERT_STATEMENT =
        "INSERT INTO milestone " +
            "(identifier, version, created_by, created_date, last_modified_by, last_modified_date, " +
            "name, type, date, header, project_id, craft_id, work_area_id, description) " +
            "VALUES (:identifier, :version, :created_by, :created_date, :last_modified_by, :last_modified_date, " +
            ":name, :type, :date, :header, :project_id, :craft_id, :work_area_id, :description)"

    private const val UPDATE_STATEMENT =
        "UPDATE milestone " +
            "SET version=:version," +
            "last_modified_by=:last_modified_by," +
            "last_modified_date=:last_modified_date," +
            "name=:name," +
            "type=:type," +
            "date=:date," +
            "header=:header," +
            "craft_id=:craft_id," +
            "work_area_id=:work_area_id," +
            "description=:description " +
            "WHERE identifier=:identifier AND version=:version-1"
  }
}
