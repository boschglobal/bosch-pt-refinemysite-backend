/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.copy.boundary

import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ActiveParticipantDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.MilestoneDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ParticipantDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ProjectCraftDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ProjectDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.RelationDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.TaskDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.WorkAreaDto
import com.bosch.pt.iot.smartsite.project.daycard.command.api.CreateDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.batch.CreateDayCardBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.message.command.api.CreateMessageCommand
import com.bosch.pt.iot.smartsite.project.message.command.handler.batch.CreateMessageBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.milestone.command.api.CreateMilestoneCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.handler.batch.CreateMilestoneBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.command.api.AssignActiveParticipantCommand
import com.bosch.pt.iot.smartsite.project.participant.command.handler.AssignActiveParticipantCommandHandler
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.command.api.CreateProjectCommand
import com.bosch.pt.iot.smartsite.project.project.command.handler.CreateProjectCommandHandler
import com.bosch.pt.iot.smartsite.project.project.query.ProjectQueryService
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.projectcraft.command.api.CreateProjectCraftCommand
import com.bosch.pt.iot.smartsite.project.projectcraft.command.handler.CreateProjectCraftCommandHandler
import com.bosch.pt.iot.smartsite.project.projectcraft.query.ProjectCraftListQueryService
import com.bosch.pt.iot.smartsite.project.relation.boundary.RelationService
import com.bosch.pt.iot.smartsite.project.task.command.api.CreateTaskCommand
import com.bosch.pt.iot.smartsite.project.task.command.api.UpdateTaskAssignmentCommand
import com.bosch.pt.iot.smartsite.project.task.command.api.UpdateTaskStatusCommand
import com.bosch.pt.iot.smartsite.project.task.command.handler.batch.CreateTaskBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskassignment.batch.AssignTaskBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.task.command.handler.taskstatus.batch.UpdateImportedTaskStatusBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.CreateTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.UpdateTaskScheduleSlotsForImportCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.batch.CreateTaskScheduleBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.projectimport.UpdateTaskScheduleSlotsForImportCommandHandler
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import com.bosch.pt.iot.smartsite.project.topic.command.api.CreateTopicCommand
import com.bosch.pt.iot.smartsite.project.topic.command.handler.batch.CreateTopicBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.workarea.command.api.CreateWorkAreaCommand
import com.bosch.pt.iot.smartsite.project.workarea.command.handler.batch.CreateWorkAreaBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.workarea.query.WorkAreaListQueryService
import datadog.trace.api.Trace
import io.opentracing.util.GlobalTracer
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@Component
class GenericImporter(
    private val assignActiveParticipantCommandHandler: AssignActiveParticipantCommandHandler,
    private val assignTaskBatchCommandHandler: AssignTaskBatchCommandHandler,
    private val createMilestoneBatchCommandHandler: CreateMilestoneBatchCommandHandler,
    private val createProjectCommandHandler: CreateProjectCommandHandler,
    private val createProjectCraftCommandHandler: CreateProjectCraftCommandHandler,
    private val createTaskBatchCommandHandler: CreateTaskBatchCommandHandler,
    private val createWorkAreaBatchCommandHandler: CreateWorkAreaBatchCommandHandler,
    private val createDayCardBatchCommandHandler: CreateDayCardBatchCommandHandler,
    private val entityManager: EntityManager,
    private val genericExporter: GenericExporter,
    private val createMessageBatchCommandHandler: CreateMessageBatchCommandHandler,
    private val projectQueryService: ProjectQueryService,
    private val projectRepository: ProjectRepository,
    private val relationService: RelationService,
    private val createTaskScheduleBatchCommandHandler: CreateTaskScheduleBatchCommandHandler,
    private val updateTaskScheduleSlotsForImportCommandHandler:
        UpdateTaskScheduleSlotsForImportCommandHandler,
    private val createTopicBatchCommandHandler: CreateTopicBatchCommandHandler,
    private val transactionTemplate: TransactionTemplate,
    private val updateImportedTaskStatusBatchCommandHandler:
        UpdateImportedTaskStatusBatchCommandHandler,
    private val workAreaListQueryService: WorkAreaListQueryService,
    private val projectCraftListQueryService: ProjectCraftListQueryService
) {

  @Trace(operationName = "import project")
  @Transactional
  fun import(project: ProjectDto, mergeStrategy: MergeStrategy = ImportEverythingMergeStrategy()) {
    createEmptyProject(project)

    // Creating a project has side-effects (such as creating an initial CSM). Whenever we
    // run an import step with such side-effects, we need to merge again: export the target
    // project and apply the merge strategy. Luckily, so far, we only need to do this after creating
    // the project initially.
    val mergedProject = mergeProjectDto(project, mergeStrategy)

    executeInTransactionWithFlushAndClear("import part 2") {
      importParticipants(mergedProject.identifier, mergedProject.participants)
      importCrafts(mergedProject.identifier, mergedProject.projectCrafts)
      importWorkAreas(mergedProject.identifier, mergedProject.workAreas)
      importMilestones(mergedProject.identifier, mergedProject.milestones)

      importTasksAsUnassignedDrafts(mergedProject.identifier, mergedProject.tasks)
      importTaskSchedules(mergedProject.tasks)

      importRelations(mergedProject.identifier, mergedProject.relations)
    }

    executeInTransactionWithFlushAndClear("import part 3") {
      importTaskAssignments(project.identifier, mergedProject.tasks)
      importDayCards(mergedProject.tasks, project.identifier)
    }

    executeInTransactionWithFlushAndClear("import part 4") {
      importTaskStatus(mergedProject.identifier, mergedProject.tasks)
    }
  }

  @Trace(operationName = "merge project")
  private fun mergeProjectDto(project: ProjectDto, mergeStrategy: MergeStrategy): ProjectDto {
    val targetProject = genericExporter.export(project.identifier)
    return mergeStrategy.merge(project, targetProject)
  }

  @Trace(operationName = "import part 1 createProject")
  private fun createEmptyProject(project: ProjectDto) {
    transactionTemplate.execute { createImportTargetProjectIfRequired(project) }
  }

  private fun createImportTargetProjectIfRequired(project: ProjectDto): ProjectId =
      if (projectRepository.findOneByIdentifier(project.identifier) == null)
          createProjectCommandHandler.handle(
              CreateProjectCommand(
                  identifier = project.identifier,
                  client = project.client,
                  description = project.description,
                  start = project.start,
                  end = project.end,
                  projectNumber = project.projectNumber,
                  title = project.title,
                  category = project.category,
                  address = project.address))
      else project.identifier

  private fun importParticipants(
      importedProjectId: ProjectId,
      participants: Set<ParticipantDto>,
  ) =
      participants
          .filterIsInstance<ActiveParticipantDto>()
          .map {
            AssignActiveParticipantCommand(
                identifier = it.identifier,
                projectRef = importedProjectId,
                companyRef = it.companyId,
                userRef = it.userId,
                role = it.role)
          }
          .forEach { assignActiveParticipantCommandHandler.handle(it) }

  private fun importCrafts(importedProjectId: ProjectId, projectCrafts: List<ProjectCraftDto>) {
    val projectCraftList = projectCraftListQueryService.findOneByProject(importedProjectId)

    projectCrafts.mapIndexed { index, projectCraft ->
      createProjectCraftCommandHandler.handle(
          CreateProjectCraftCommand(
              projectIdentifier = importedProjectId,
              identifier = projectCraft.identifier,
              name = projectCraft.name,
              color = projectCraft.color,
              projectCraftListVersion = projectCraftList.version + index.toLong(),
              position = null))
    }
  }

  private fun importWorkAreas(
      importedProjectId: ProjectId,
      workAreas: List<WorkAreaDto>,
  ) {
    if (workAreas.isEmpty()) return
    requireNotNull(
        workAreaListQueryService.findOneWithDetailsByProjectIdentifier(importedProjectId))
    requireNotNull(projectQueryService.findOneByIdentifier(importedProjectId))

    val createWorkAreaCommands =
        workAreas.mapIndexed { index, workArea ->
          CreateWorkAreaCommand(
              workArea.identifier, importedProjectId, workArea.name, null, index.toLong())
        }

    createWorkAreaBatchCommandHandler.handle(importedProjectId, createWorkAreaCommands)
  }

  private fun importMilestones(
      importedProjectId: ProjectId,
      milestones: List<MilestoneDto>,
  ) =
      milestones
          .map {
            CreateMilestoneCommand(
                identifier = it.identifier,
                projectRef = importedProjectId,
                name = it.name,
                type = it.type,
                date = it.date,
                header = it.header,
                description = it.description,
                craftRef = it.projectCraft,
                workAreaRef = it.workArea)
          }
          .let { createMilestoneBatchCommandHandler.handle(importedProjectId, it) }

  private fun importTasksAsUnassignedDrafts(
      importedProjectId: ProjectId,
      tasks: List<TaskDto>,
  ) {
    tasks
        .map {
          CreateTaskCommand(
              identifier = it.identifier,
              projectIdentifier = importedProjectId,
              name = it.name,
              description = it.description,
              location = it.location,
              status = DRAFT,
              projectCraftIdentifier = it.projectCraft,
              assigneeIdentifier = null,
              workAreaIdentifier = it.workArea)
        }
        .let { createTaskBatchCommandHandler.handle(it) }

    importTaskTopics(importedProjectId, tasks)
  }

  private fun importTaskTopics(importedProjectId: ProjectId, tasks: List<TaskDto>) {
    tasks
        .filter { it.topics.isNotEmpty() }
        .forEach { task ->
          createTopicBatchCommandHandler.handle(
              projectIdentifier = importedProjectId,
              commands =
                  task.topics.map {
                    CreateTopicCommand(
                        it.identifier,
                        it.criticality,
                        it.description,
                        task.identifier,
                        importedProjectId)
                  })

          task.topics
              .filter { it.messages.isNotEmpty() }
              .forEach { topic ->
                createMessageBatchCommandHandler.handle(
                    topic.messages.map {
                      CreateMessageCommand(
                          identifier = it.identifier,
                          content = it.content!!,
                          topicIdentifier = topic.identifier,
                          projectIdentifier = importedProjectId)
                    })
              }
        }
  }

  private fun importTaskSchedules(tasks: List<TaskDto>) =
      tasks
          .filter { it.start != null || it.end != null }
          .map {
            CreateTaskScheduleCommand(
                identifier = TaskScheduleId(),
                taskIdentifier = it.identifier,
                start = it.start,
                end = it.end,
                slots = null)
          }
          .let { taskSchedules -> createTaskScheduleBatchCommandHandler.handle(taskSchedules) }

  private fun importTaskAssignments(projectId: ProjectId, tasks: List<TaskDto>) {
    tasks
        .filter { it.assignee != null }
        .map { UpdateTaskAssignmentCommand(it.identifier, it.assignee!!) }
        .let { assignTaskBatchCommandHandler.handle(projectIdentifier = projectId, commands = it) }
  }

  private fun importTaskStatus(importedProjectId: ProjectId, tasks: List<TaskDto>) {
    tasks
        .filter { it.status != DRAFT }
        .map { UpdateTaskStatusCommand(it.identifier, it.status) }
        .let { updateImportedTaskStatusBatchCommandHandler.handle(importedProjectId, it) }
  }

  private fun importDayCards(tasks: List<TaskDto>, projectIdentifier: ProjectId) {
    tasks.forEach { task ->
      task.dayCards
          .map {
            CreateDayCardCommand(
                identifier = it.identifier,
                taskIdentifier = task.identifier,
                title = it.title,
                manpower = it.manpower,
                notes = it.notes,
                status = it.status,
                reason = it.reason)
          }
          .let { createDayCardCommands ->
            createDayCardBatchCommandHandler.handle(createDayCardCommands, projectIdentifier)
          }

      if (task.dayCards.isNotEmpty()) {
        updateTaskScheduleSlotsForImportCommandHandler.handle(
            UpdateTaskScheduleSlotsForImportCommand(
                taskIdentifier = task.identifier,
                slots = task.dayCards.associate { it.identifier to it.date }))
      }
    }
  }

  private fun importRelations(
      importedProjectId: ProjectId,
      relations: List<RelationDto>,
  ) =
      relations.forEach {
        relationService.create(
            com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto(
                source =
                    com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto
                        .RelationElementDto(id = it.source.id, type = it.source.type),
                target =
                    com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto
                        .RelationElementDto(id = it.target.id, type = it.target.type),
                type = it.type),
            importedProjectId)
      }

  @Trace
  private fun <T> executeInTransactionWithFlushAndClear(traceSpan: String, block: () -> T): T =
      transactionTemplate
          .execute { block.invoke() }
          .also {
            entityManager.flush()
            entityManager.clear()
          }!!
          .also { GlobalTracer.get().activeSpan().setOperationName(traceSpan) }

  /**
   * Provides a strategy for merging a source Project into an existing target Project. The purpose
   * of the merge is to resolve any conflicts on later import steps. Current [MergeStrategy]
   * implementations are purely additive. In case we later need to remove or replace entities,
   * [MergeStrategy] needs to return a diff, and the [GenericImporter] would have to apply this diff
   * accordingly.
   */
  interface MergeStrategy {
    fun merge(source: ProjectDto, target: ProjectDto): ProjectDto
  }

  /**
   * Simply merges everything without conflict resolution. Imports will fail with this
   * [MergeStrategy], if, for example, entities with the same identifiers already exist.
   */
  class ImportEverythingMergeStrategy : MergeStrategy {
    override fun merge(source: ProjectDto, target: ProjectDto): ProjectDto {
      return source
    }
  }
}
