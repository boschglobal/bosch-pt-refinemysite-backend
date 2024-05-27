/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.external.boundary

import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdTypeEnumAvro.MS_PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdTypeEnumAvro.P6
import com.bosch.pt.csm.cloud.projectmanagement.importer.messages.ProjectImportFinishedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.importer.messages.ProjectImportStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.simulateKafkaListener
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.test.FileSource
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportParameters
import com.bosch.pt.iot.smartsite.project.exporter.boundary.ProjectExportService
import com.bosch.pt.iot.smartsite.project.importer.boundary.AbstractImportIntegrationTest
import com.bosch.pt.iot.smartsite.project.importer.boundary.ProjectImportService
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.milestone.command.api.DeleteMilestoneCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.handler.DeleteMilestoneCommandHandler
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.command.api.DeleteProjectCommand
import com.bosch.pt.iot.smartsite.project.project.command.handler.DeleteProjectCommandHandler
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectController
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.task.command.api.DeleteTaskCommand
import com.bosch.pt.iot.smartsite.project.task.command.handler.DeleteTaskCommandHandler
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.workarea.command.api.DeleteWorkAreaCommand
import com.bosch.pt.iot.smartsite.project.workarea.command.handler.DeleteWorkAreaCommandHandler
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource

@EnableAllKafkaListeners
class DeleteExternalIdInRmsIntegrationTest : AbstractImportIntegrationTest() {

  @Autowired private lateinit var projectImportService: ProjectImportService

  @Autowired private lateinit var projectExportService: ProjectExportService

  @Autowired private lateinit var projectRepository: ProjectRepository

  @Autowired private lateinit var deleteProjectCommandHandler: DeleteProjectCommandHandler

  @Autowired private lateinit var taskRepository: TaskRepository

  @Autowired private lateinit var deleteTaskCommandHandler: DeleteTaskCommandHandler

  @Autowired private lateinit var milestoneRepository: MilestoneRepository

  @Autowired private lateinit var deleteMilestoneCommandHandler: DeleteMilestoneCommandHandler

  @Autowired private lateinit var workAreaRepository: WorkAreaRepository

  @Autowired private lateinit var deleteWorkAreaCommandHandler: DeleteWorkAreaCommandHandler

  @Autowired private lateinit var projectController: ProjectController

  private val projectIdentifier = ProjectId()
  private val project by lazy { projectRepository.findOneByIdentifier(projectIdentifier)!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData().submitProjectImportFeatureToggle()

    setAuthentication("userCsm1")
    projectController.createProject(projectIdentifier, createDefaultSaveProjectResource())

    projectEventStoreUtils.reset()
  }

  @FileSource(
      container = "project-import-testdata", files = ["work-area-with-task-and-milestone.mpp"])
  @ParameterizedTest
  fun `verify deletion of project deletes external ids as well`(file: Resource) {

    // Import from file to ensure that we have all external ids
    projectImportService.import(project, readProject(file), true, null, null, null, null)
    // Export as different format to have multiple external ids per object (i.e. one per format)
    projectExportService.export(
        project, ProjectExportParameters(ProjectExportFormatEnum.PRIMAVERA_P6_XML, true, true))
    verifyKafkaEvents()
    projectEventStoreUtils.reset()

    // Load data before deleting to compare it later
    val milestone = milestoneRepository.findAllByProjectIdentifier(projectIdentifier).single()
    val task = taskRepository.findAllByProjectIdentifier(projectIdentifier).first()
    val workArea = workAreaRepository.findAllByProjectIdentifier(projectIdentifier).single()

    // Delete the project
    simulateKafkaListener {
      setAuthentication("userCsm1")
      deleteProjectCommandHandler.handle(DeleteProjectCommand(projectIdentifier))
    }

    // Check that external id deleted events exist
    val deletedExternalIds =
        projectEventStoreUtils.verifyContainsAndGet(
            ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.DELETED, 6, false)

    verifyExternalIds(deletedExternalIds, milestone, task, workArea)
  }

  @FileSource(
      container = "project-import-testdata", files = ["work-area-with-task-and-milestone.mpp"])
  @ParameterizedTest
  fun `verify deletion of work area, task and milestone deletes external ids as well`(
      file: Resource
  ) {

    // Import from file to ensure that we have all external ids
    projectImportService.import(project, readProject(file), true, null, null, null, null)
    // Export as different format to have multiple external ids per object (i.e. one per format)
    projectExportService.export(
        project, ProjectExportParameters(ProjectExportFormatEnum.PRIMAVERA_P6_XML, true, true))
    verifyKafkaEvents()
    projectEventStoreUtils.reset()

    // Delete data in order
    val milestone = milestoneRepository.findAllByProjectIdentifier(projectIdentifier).single()
    deleteMilestoneCommandHandler.handle(
        DeleteMilestoneCommand(milestone.identifier, milestone.version))

    val task = taskRepository.findAllByProjectIdentifier(projectIdentifier).first()
    simulateKafkaListener {
      setAuthentication("userCsm1")
      deleteTaskCommandHandler.handle(DeleteTaskCommand(task.identifier))
    }

    val workArea = workAreaRepository.findAllByProjectIdentifier(projectIdentifier).single()
    deleteWorkAreaCommandHandler.handle(
        DeleteWorkAreaCommand(workArea.identifier, workArea.version))

    // Check that external id deleted events exist
    val deletedExternalIds =
        projectEventStoreUtils.verifyContainsAndGet(
            ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.DELETED, 6, false)

    verifyExternalIds(deletedExternalIds, milestone, task, workArea)
  }

  private fun verifyExternalIds(
      deletedExternalIds: List<ExternalIdEventAvro>,
      milestone: Milestone,
      task: Task,
      workArea: WorkArea
  ) {
    val milestoneExternalIds =
        deletedExternalIds.filter {
          it.aggregate.objectIdentifier.identifier == milestone.identifier.identifier.toString()
        }
    assertThat(milestoneExternalIds).hasSize(2)
    assertThat(milestoneExternalIds.map { it.aggregate.type }).containsAll(listOf(MS_PROJECT, P6))

    val taskExternalIds =
        deletedExternalIds.filter {
          it.aggregate.objectIdentifier.identifier == task.identifier.identifier.toString()
        }
    assertThat(taskExternalIds).hasSize(2)
    assertThat(taskExternalIds.map { it.aggregate.type }).containsAll(listOf(MS_PROJECT, P6))

    val workAreaExternalIds =
        deletedExternalIds.filter {
          it.aggregate.objectIdentifier.identifier == workArea.identifier.identifier.toString()
        }
    assertThat(workAreaExternalIds).hasSize(2)
    assertThat(workAreaExternalIds.map { it.aggregate.type }).containsAll(listOf(MS_PROJECT, P6))
  }

  private fun verifyKafkaEvents() {
    projectEventStoreUtils
        .verifyContainsAndGet(
            WorkAreaEventAvro::class.java, WorkAreaEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "w1" }

    projectEventStoreUtils.verifyContains(
        WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.ITEMADDED, 1, false)

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "t1" }

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "m1" }

    projectEventStoreUtils.verifyContains(
        MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 1, false)

    projectEventStoreUtils.verifyContains(
        RelationEventAvro::class.java, RelationEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 6, false)

    projectEventStoreUtils.verifyNumberOfEvents(17)
  }
}
