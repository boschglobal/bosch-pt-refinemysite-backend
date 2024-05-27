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
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.MILESTONELIST
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro.ITEMADDED
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro.ITEMREMOVED
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro.REORDERED
import com.bosch.pt.iot.smartsite.common.i18n.Key.MILESTONE_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextSnapshotStore
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneListId
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneListId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneList
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneListRepository
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
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
class MilestoneListSnapshotStore(
    private val snapshotCache: MilestoneListSnapshotEntityCache,
    private val milestoneListRepository: MilestoneListRepository,
    private val projectRepository: ProjectRepository,
    private val workAreRepository: WorkAreaRepository,
    namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    em: EntityManager,
    logger: Logger
) :
    AbstractSnapshotStoreJdbc<
        MilestoneListEventAvro, MilestoneListSnapshot, MilestoneList, MilestoneListId>(
        namedParameterJdbcTemplate, em, logger),
    ProjectContextSnapshotStore {

  override fun findOrFail(identifier: MilestoneListId) =
      snapshotCache.get(identifier)?.asSnapshot()
          ?: throw AggregateNotFoundException(
              MILESTONE_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

  override fun findInternal(identifier: UUID) = snapshotCache.get(identifier.asMilestoneListId())

  override fun isDeletedEvent(message: SpecificRecordBase) =
      (message as MilestoneListEventAvro).name == DELETED

  override fun updateInternal(
      event: MilestoneListEventAvro,
      currentSnapshot: MilestoneList?,
      rootContextIdentifier: UUID
  ) {
    if (event.name == DELETED && currentSnapshot != null) {
      deleteMilestoneList(currentSnapshot)
      snapshotCache.remove(currentSnapshot.identifier)
    } else {
      when (currentSnapshot == null) {
        true -> createMilestoneList(event.aggregate)
        false -> {
          updateMilestoneList(event.aggregate)
          removeFromPersistenceContext(currentSnapshot)
          snapshotCache.remove(currentSnapshot.identifier)
        }
      }
    }
  }

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase) =
      key.aggregateIdentifier.type == MILESTONELIST.value &&
          message is MilestoneListEventAvro &&
          message.name in setOf(CREATED, ITEMADDED, ITEMREMOVED, REORDERED, DELETED)

  private fun createMilestoneList(aggregate: MilestoneListAggregateAvro) {
    with(aggregate) {
      val projectId = findProjectIdOrFail(project)
      val workAreaId = aggregate.workarea?.let { findWorkAreaIdOrFail(aggregate.workarea) }

      MapSqlParameterSource()
          .addValue("identifier", aggregateIdentifier.identifier)
          .addValue("created_by", auditingInformation.getCreatedByIdentifier().toString())
          .addValue("created_date", Timestamp(auditingInformation.createdDate))
          .addValue("version", aggregateIdentifier.version)
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .addValue("project_id", projectId)
          .addValue("date", Timestamp(aggregate.date))
          .addValue("header", aggregate.header)
          .addValue("work_area_id", workAreaId)
          .addValue("work_area_id_constraint", workAreaId ?: -1)
          .apply { execute(INSERT_STATEMENT, this) }

      updateMilestonePositions(aggregate)
    }
  }

  private fun updateMilestoneList(aggregate: MilestoneListAggregateAvro) {
    with(aggregate) {
      MapSqlParameterSource()
          .addValue("identifier", aggregateIdentifier.identifier)
          .addValue("version", aggregateIdentifier.version)
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .apply { execute(UPDATE_STATEMENT, this) }

      updateMilestonePositions(aggregate)
    }
  }

  private fun deleteMilestoneList(milestoneList: MilestoneList) =
      milestoneListRepository.delete(milestoneList)

  private fun updateMilestonePositions(aggregate: MilestoneListAggregateAvro) {
    val milestoneListId = findMilestoneListIdOrFail(aggregate.aggregateIdentifier)
    with(aggregate) {
      milestones.forEachIndexed { index, milestone ->
        MapSqlParameterSource()
            .addValue("identifier", milestone.identifier)
            .addValue("position", index)
            .addValue("milestone_list_id", milestoneListId)
            .apply { execute(UPDATE_MILESTONE_POSITION_STATEMENT, this) }
      }
    }
  }

  private fun findProjectIdOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): Long =
      requireNotNull(
          projectRepository.findIdByIdentifier(aggregateIdentifierAvro.identifier.asProjectId())) {
            "Could not find Project ${aggregateIdentifierAvro.identifier}"
          }

  private fun findWorkAreaIdOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): Long =
      requireNotNull(
          workAreRepository.findIdByIdentifier(aggregateIdentifierAvro.identifier.asWorkAreaId())) {
            "Could not find WorkArea ${aggregateIdentifierAvro.identifier}"
          }

  private fun findMilestoneListIdOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): Long =
      requireNotNull(
          milestoneListRepository.findIdByIdentifier(
              aggregateIdentifierAvro.identifier.asMilestoneListId())) {
            "Could not find MilestoneList ${aggregateIdentifierAvro.identifier}"
          }

  companion object {

    private const val INSERT_STATEMENT =
        "INSERT INTO milestone_list " +
            "(identifier, version, created_by, created_date, last_modified_by, last_modified_date, " +
            "date, header, project_id, work_area_id, work_area_id_constraint) " +
            "VALUES (:identifier, :version, :created_by, :created_date, :last_modified_by, :last_modified_date, " +
            ":date, :header, :project_id, :work_area_id, :work_area_id_constraint)"

    private const val UPDATE_STATEMENT =
        "UPDATE milestone_list " +
            "SET version=:version, " +
            "last_modified_by=:last_modified_by, " +
            "last_modified_date=:last_modified_date " +
            "WHERE identifier=:identifier AND version=:version-1"

    private const val UPDATE_MILESTONE_POSITION_STATEMENT =
        "UPDATE milestone " +
            "SET position=:position," +
            "milestone_list_id=:milestone_list_id " +
            "WHERE identifier=:identifier"
  }
}
