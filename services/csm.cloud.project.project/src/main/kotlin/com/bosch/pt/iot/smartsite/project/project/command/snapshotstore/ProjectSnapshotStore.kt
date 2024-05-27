/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.project.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJdbc
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.messages.getCreatedByIdentifier
import com.bosch.pt.csm.cloud.common.messages.getLastModifiedByIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.attachment.boundary.BlobStoreService
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_VALIDATION_ERROR_PROJECT_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextSnapshotStore
import com.bosch.pt.iot.smartsite.project.importer.boundary.ProjectImportDeleteService
import com.bosch.pt.iot.smartsite.project.milestone.command.service.MilestoneDeleteService
import com.bosch.pt.iot.smartsite.project.participant.shared.boundary.ParticipantDeleteService
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.projectcraft.command.service.ProjectCraftDeleteService
import com.bosch.pt.iot.smartsite.project.projectpicture.boundary.ProjectPictureService
import com.bosch.pt.iot.smartsite.project.quickfilter.boundary.QuickFilterService
import com.bosch.pt.iot.smartsite.project.relation.boundary.RelationService
import com.bosch.pt.iot.smartsite.project.rfv.boundary.RfvService
import com.bosch.pt.iot.smartsite.project.task.command.service.TaskDeleteService
import com.bosch.pt.iot.smartsite.project.taskconstraint.boundary.TaskConstraintService
import com.bosch.pt.iot.smartsite.project.workarea.command.service.WorkAreaDeleteService
import com.bosch.pt.iot.smartsite.project.workday.boundary.WorkdayConfigurationDeleteService
import java.sql.Timestamp
import java.util.UUID
import jakarta.persistence.EntityManager
import org.apache.avro.specific.SpecificRecordBase
import org.slf4j.Logger
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class ProjectSnapshotStore(
    private val snapshotCache: ProjectSnapshotEntityCache,
    private val projectRepository: ProjectRepository,
    private val participantDeleteService: ParticipantDeleteService,
    private val projectCraftDeleteService: ProjectCraftDeleteService,
    private val workdayConfigurationDeleteService: WorkdayConfigurationDeleteService,
    private val workAreaDeleteService: WorkAreaDeleteService,
    private val projectImportDeleteService: ProjectImportDeleteService,
    private val projectPictureService: ProjectPictureService,
    private val taskDeleteService: TaskDeleteService,
    private val milestoneDeleteService: MilestoneDeleteService,
    private val taskConstraintService: TaskConstraintService,
    private val quickFilterService: QuickFilterService,
    private val rfvService: RfvService,
    private val relationService: RelationService,
    private val blobStoreService: BlobStoreService,
    namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    em: EntityManager,
    logger: Logger
) :
    AbstractSnapshotStoreJdbc<ProjectEventAvro, ProjectSnapshot, Project, ProjectId>(
        namedParameterJdbcTemplate, em, logger),
    ProjectContextSnapshotStore {

  override fun findOrFail(identifier: ProjectId) =
      snapshotCache.get(identifier)?.asSnapshot()
          ?: throw AggregateNotFoundException(
              PROJECT_VALIDATION_ERROR_PROJECT_NOT_FOUND, identifier.toString())

  fun findOrIgnore(identifier: ProjectId) = snapshotCache.get(identifier)?.asSnapshot()

  override fun findInternal(identifier: UUID) = snapshotCache.get(identifier.asProjectId())

  override fun isDeletedEvent(message: SpecificRecordBase) =
      (message as ProjectEventAvro).name == DELETED

  override fun updateInternal(
      event: ProjectEventAvro,
      currentSnapshot: Project?,
      rootContextIdentifier: UUID
  ) {
    if (event.name == DELETED && currentSnapshot != null) {
      deleteProject(currentSnapshot)
      snapshotCache.remove(currentSnapshot.identifier)
    } else {
      when (currentSnapshot == null) {
        true -> createProject(event.aggregate)
        false -> {
          updateProject(event.aggregate)
          removeFromPersistenceContext(currentSnapshot)
          snapshotCache.remove(currentSnapshot.identifier)
        }
      }
    }
  }

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase) =
      key.aggregateIdentifier.type == PROJECT.value &&
          message is ProjectEventAvro &&
          message.name in setOf(CREATED, UPDATED, DELETED)

  private fun createProject(aggregate: ProjectAggregateAvro) {
    with(aggregate) {
      MapSqlParameterSource()
          .addValue("identifier", aggregateIdentifier.identifier)
          .addValue("created_by", auditingInformation.getCreatedByIdentifier().toString())
          .addValue("created_date", Timestamp(auditingInformation.createdDate))
          .addValue("version", aggregateIdentifier.version)
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .addValue("client", client)
          .addValue(
              "category",
              if (category != null) ProjectCategoryEnum.valueOf(category.name).name else null)
          .addValue("description", description)
          .addValue("project_start", Timestamp(start))
          .addValue("project_end", Timestamp(end))
          .addValue("project_number", projectNumber)
          .addValue("title", title)
          .addValue("street", projectAddress?.street)
          .addValue("house_number", projectAddress?.houseNumber)
          .addValue("zip_code", projectAddress?.zipCode)
          .addValue("city", projectAddress?.city)
          .apply { execute(INSERT_STATEMENT, this) }
    }
  }

  private fun updateProject(aggregate: ProjectAggregateAvro) {
    with(aggregate) {
      MapSqlParameterSource()
          .addValue("identifier", aggregateIdentifier.identifier)
          .addValue("version", aggregateIdentifier.version)
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .addValue("client", client)
          .addValue(
              "category",
              if (category != null) ProjectCategoryEnum.valueOf(category.name).name else null)
          .addValue("description", description)
          .addValue("project_start", Timestamp(start))
          .addValue("project_end", Timestamp(end))
          .addValue("project_number", projectNumber)
          .addValue("title", title)
          .addValue("street", projectAddress?.street)
          .addValue("house_number", projectAddress?.houseNumber)
          .addValue("zip_code", projectAddress?.zipCode)
          .addValue("city", projectAddress?.city)
          .apply { execute(UPDATE_STATEMENT, this) }
    }
  }

  private fun deleteProject(project: Project) {
    projectImportDeleteService.deleteByProjectIdentifier(project.identifier)
    taskDeleteService.deletePartitioned(project.id!!)
    milestoneDeleteService.deleteByProjectId(project.id!!)
    projectCraftDeleteService.deleteByProjectId(project.id!!)
    workdayConfigurationDeleteService.deleteByProjectId(project.id!!)
    workAreaDeleteService.deleteByProjectId(project.id!!)
    participantDeleteService.deleteByProjectId(project.id!!)
    projectPictureService.deleteByProjectIdentifier(project.identifier)
    blobStoreService.deleteImagesInDirectory(project.identifier.toString())
    taskConstraintService.deleteConstraintCustomizationsByProjectIdendtifier(project.identifier)
    rfvService.deleteRfvCustomizationsByProjectIdentifier(project.identifier)
    quickFilterService.deleteAllByProjectIdentifier(project.identifier)
    relationService.deleteByProjectId(project.id!!)

    // delete project
    projectRepository.deleteById(project.id!!)
  }

  companion object {
    private const val INSERT_STATEMENT =
        "INSERT INTO project (identifier, version, created_by, created_date, last_modified_by, last_modified_date," +
            "client, category, description, project_start, project_end, " +
            "project_number, title, street, house_number, zip_code, city) " +
            "VALUES (:identifier, :version, :created_by, :created_date, :last_modified_by, :last_modified_date, " +
            ":client, :category, :description, :project_start, :project_end, " +
            ":project_number, :title, :street, :house_number, :zip_code, :city);"

    private const val UPDATE_STATEMENT =
        "UPDATE project " +
            "SET version=:version," +
            "last_modified_by=:last_modified_by," +
            "last_modified_date=:last_modified_date," +
            "client=:client," +
            "category=:category," +
            "description=:description," +
            "project_start=:project_start," +
            "project_end=:project_end," +
            "project_number=:project_number," +
            "title=:title," +
            "street=:street," +
            "house_number=:house_number," +
            "zip_code=:zip_code," +
            "city=:city " +
            "WHERE identifier=:identifier AND version=:version-1"
  }
}
