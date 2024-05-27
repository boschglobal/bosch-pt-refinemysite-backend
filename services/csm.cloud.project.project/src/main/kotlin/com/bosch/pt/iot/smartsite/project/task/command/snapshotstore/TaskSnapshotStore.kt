/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJdbc
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.getCreatedByIdentifier
import com.bosch.pt.csm.cloud.common.messages.getLastModifiedByIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASK
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.ACCEPTED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.ASSIGNED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.CLOSED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.RESET
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.SENT
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.STARTED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.UNASSIGNED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.attachment.boundary.BlobStoreService
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_PROJECT_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextSnapshotStore
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftRepository
import com.bosch.pt.iot.smartsite.project.relation.boundary.RelationService
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.taskattachment.boundary.TaskAttachmentService
import com.bosch.pt.iot.smartsite.project.taskconstraint.boundary.TaskConstraintSelectionService
import com.bosch.pt.iot.smartsite.project.taskschedule.command.service.TaskScheduleDeleteService
import com.bosch.pt.iot.smartsite.project.topic.boundary.TopicDeleteService
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import jakarta.persistence.EntityManager
import java.sql.Timestamp
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.slf4j.Logger
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class TaskSnapshotStore(
    private val snapshotCache: TaskSnapshotEntityCache,
    private val repository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val taskConstraintSelectionService: TaskConstraintSelectionService,
    private val topicDeleteService: TopicDeleteService,
    private val taskAttachmentService: TaskAttachmentService,
    private val taskScheduleDeleteService: TaskScheduleDeleteService,
    private val relationService: RelationService,
    private val blobStoreService: BlobStoreService,
    private val workAreRepository: WorkAreaRepository,
    private val participantRepository: ParticipantRepository,
    private val projectCraftRepository: ProjectCraftRepository,
    namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    em: EntityManager,
    logger: Logger
) :
    AbstractSnapshotStoreJdbc<TaskEventAvro, TaskSnapshot, Task, TaskId>(
        namedParameterJdbcTemplate, em, logger),
    ProjectContextSnapshotStore {

  override fun findOrFail(identifier: TaskId) =
      snapshotCache.get(identifier)?.asSnapshot()
          ?: throw AggregateNotFoundException(
              TASK_VALIDATION_ERROR_PROJECT_NOT_FOUND, identifier.toString())

  fun findAllOrIgnore(identifiers: List<TaskId>) =
      snapshotCache.populateFromCall { snapshotCache.loadAllFromDatabase(identifiers) }

  fun findOrIgnore(identifier: TaskId) = snapshotCache.get(identifier)?.asSnapshot()

  override fun findInternal(identifier: UUID) = snapshotCache.get(identifier.asTaskId())

  override fun isDeletedEvent(message: SpecificRecordBase) =
      (message as TaskEventAvro).name == DELETED

  override fun updateInternal(
      event: TaskEventAvro,
      currentSnapshot: Task?,
      rootContextIdentifier: UUID
  ) {
    if (event.name == DELETED && currentSnapshot != null) {
      deleteTask(currentSnapshot)
      snapshotCache.remove(currentSnapshot.identifier)
    } else {
      when (currentSnapshot == null) {
        true -> createTask(event.aggregate)
        false -> {
          updateTask(event.aggregate)
          removeFromPersistenceContext(currentSnapshot)
          snapshotCache.remove(currentSnapshot.identifier)
        }
      }
    }
  }

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase) =
      key.aggregateIdentifier.type == TASK.value &&
          message is TaskEventAvro &&
          message.name in
              setOf(
                  CREATED,
                  UPDATED,
                  ASSIGNED,
                  UNASSIGNED,
                  CLOSED,
                  STARTED,
                  SENT,
                  DELETED,
                  RESET,
                  ACCEPTED)

  private fun createTask(aggregate: TaskAggregateAvro) {
    val workAreaId = aggregate.workarea?.let { findWorkAreaIdOrFail(aggregate.workarea) }
    val assigneeId = aggregate.assignee?.let { findParticipantId(aggregate.assignee) }
    val projectCraftId = findProjectCraftIdOrFail(aggregate.craft)

    with(aggregate) {
      val projectId = findProjectIdOrFail(project)
      MapSqlParameterSource()
          .addValue("identifier", aggregateIdentifier.identifier)
          .addValue("version", aggregateIdentifier.version)
          .addValue("created_by", auditingInformation.getCreatedByIdentifier().toString())
          .addValue("created_date", Timestamp(auditingInformation.createdDate))
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .addValue("project_id", projectId)
          .addValue("name", name)
          .addValue("description", description)
          .addValue("location", location)
          .addValue("project_craft_id", projectCraftId)
          .addValue("assignee_id", assigneeId)
          .addValue("work_area_id", workAreaId)
          .addValue("status", TaskStatusEnum.valueOf(status.name).getPosition())
          .addValue("edit_date", editDate?.let { Timestamp(editDate) })
          .apply { execute(INSERT_STATEMENT, this) }
    }
  }

  private fun updateTask(aggregate: TaskAggregateAvro) {
    val workAreaId = aggregate.workarea?.let { findWorkAreaIdOrFail(aggregate.workarea) }
    val assigneeId = aggregate.assignee?.let { findParticipantId(aggregate.assignee) }
    val projectCraftId = findProjectCraftIdOrFail(aggregate.craft)

    with(aggregate) {
      MapSqlParameterSource()
          .addValue("identifier", aggregateIdentifier.identifier)
          .addValue("version", aggregateIdentifier.version)
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .addValue("name", name)
          .addValue("description", description)
          .addValue("location", location)
          .addValue("project_craft_id", projectCraftId)
          .addValue("assignee_id", assigneeId)
          .addValue("work_area_id", workAreaId)
          .addValue("status", TaskStatusEnum.valueOf(status.name).getPosition())
          .addValue("edit_date", Timestamp(Date.from(Instant.now()).time))
          .apply { execute(UPDATE_STATEMENT, this) }
    }
  }

  private fun deleteTask(task: Task) {
    val taskIds = listOf(task.id!!)
    taskConstraintSelectionService.deletePartitioned(taskIds)
    topicDeleteService.deletePartitioned(taskIds)
    taskAttachmentService.deletePartitioned(taskIds)
    taskScheduleDeleteService.deletePartitioned(taskIds)
    relationService.deleteByTaskIdentifier(task.identifier.toUuid())

    // safety net to remove blobs that might have been uploaded but never stored as attachment
    // resolutions here might be caused due to missed image scaling events (rare cases anyway)
    blobStoreService.deleteImagesInDirectory(task.identifier.toString())

    repository.delete(task)

    logger.info("Task {} was deleted", task.identifier.toUuid())
  }

  private fun findProjectIdOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): Long =
      requireNotNull(
          projectRepository.findIdByIdentifier(aggregateIdentifierAvro.identifier.asProjectId())) {
            "Could not find ProjectCraft ${aggregateIdentifierAvro.identifier.asProjectId()}"
          }

  private fun findWorkAreaIdOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): Long? =
      workAreRepository.findIdByIdentifier(aggregateIdentifierAvro.identifier.asWorkAreaId())

  private fun findProjectCraftIdOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): Long =
      requireNotNull(
          projectCraftRepository.findIdByIdentifier(
              aggregateIdentifierAvro.identifier.asProjectCraftId())) {
            "Could not find ProjectCraft ${aggregateIdentifierAvro.identifier}"
          }

  private fun findParticipantId(aggregateIdentifierAvro: AggregateIdentifierAvro): Long? =
      participantRepository.findIdByIdentifier(aggregateIdentifierAvro.identifier.asParticipantId())

  companion object {
    private const val INSERT_STATEMENT =
        "INSERT INTO task (identifier, version, created_by, created_date, last_modified_by, last_modified_date, " +
            "project_id, name, description, location, project_craft_id, edit_date, " +
            "assignee_id, work_area_id, status) " +
            "VALUES (:identifier, :version, :created_by, :created_date, :last_modified_by, :last_modified_date, " +
            ":project_id, :name, :description, :location, :project_craft_id, :edit_date, " +
            ":assignee_id, :work_area_id, :status);"

    private const val UPDATE_STATEMENT =
        "UPDATE task " +
            "SET version=:version," +
            "last_modified_by=:last_modified_by," +
            "last_modified_date=:last_modified_date," +
            "name=:name," +
            "description=:description," +
            "location=:location," +
            "project_craft_id=:project_craft_id," +
            "assignee_id=:assignee_id," +
            "work_area_id=:work_area_id," +
            "status=:status," +
            "edit_date=:edit_date " +
            "WHERE identifier=:identifier AND version=:version-1"
  }
}
