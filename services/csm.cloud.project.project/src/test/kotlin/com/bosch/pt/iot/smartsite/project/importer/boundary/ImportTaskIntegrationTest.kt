/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary

import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.importer.messages.ProjectImportFinishedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.importer.messages.ProjectImportStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.ACCEPTED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.STARTED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.test.FileSource
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectController
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task.Companion.MAX_DESCRIPTION_LENGTH
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task.Companion.MAX_NAME_LENGTH
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.data.domain.Pageable.unpaged

@EnableAllKafkaListeners
class ImportTaskIntegrationTest : AbstractImportIntegrationTest() {

  @Autowired private lateinit var projectImportService: ProjectImportService

  @Autowired private lateinit var projectRepository: ProjectRepository

  @Autowired private lateinit var taskRepository: TaskRepository

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
      container = "project-import-testdata",
      files =
          [
              "task-with-empty-name.pp",
              "task-with-empty-name.mpp",
              "task-with-empty-name.xer",
              "task-with-empty-name-ms.xml",
              "task-with-empty-name-p6.xml"])
  @ParameterizedTest
  fun `verify a task with empty name is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), true, null, null, null, null)

    val tasks = taskRepository.findAllByProjectIdentifier(projectIdentifier, unpaged()).content

    tasks.apply {
      assertThat(this).hasSize(1)
      assertThat(this.first().name).isEqualTo("Unnamed Task")
    }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "Unnamed Task" }

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyNumberOfEvents(7)
  }

  // In P6 you cannot create tasks without names.
  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "task-2-tasks-without-name.pp",
              "task-2-tasks-without-name.mpp",
              "task-2-tasks-without-name-ms.xml"])
  @ParameterizedTest
  fun `verify a task without name is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), true, null, null, null, null)

    val tasks = taskRepository.findAllByProjectIdentifier(projectIdentifier, unpaged()).content

    tasks.apply {
      assertThat(this).hasSize(2)
      assertThat(this.first().name).isEqualTo("Unnamed Task")
    }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, CREATED, 2, false)
        .map { it.aggregate.name }
        .all { listOf("Unnamed Task", "<New Task>").contains(it) }

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 2, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 2, false)

    projectEventStoreUtils.verifyNumberOfEvents(10)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "task-with-name-too-long.pp",
              "task-with-name-too-long.mpp",
              "task-with-name-too-long.xer",
              "task-with-name-too-long-ms.xml",
              "task-with-name-too-long-p6.xml"])
  @ParameterizedTest
  fun `verify a task with too long name is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), true, null, null, null, null)

    val tasks = taskRepository.findAllByProjectIdentifier(projectIdentifier, unpaged()).content

    tasks.apply {
      assertThat(this).hasSize(1)
      assertThat(this.first().name)
          .isEqualTo("Task name is too long. ".repeat(5).take(MAX_NAME_LENGTH))
    }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it.startsWith("Task name is too long.") }

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyNumberOfEvents(7)
  }

  // There doesn't seem to be a "notes" column in P6, therefore we don't test it.
  @FileSource(
      container = "project-import-testdata",
      files = ["task-with-note.pp", "task-with-note.mpp", "task-with-note-ms.xml"])
  @ParameterizedTest
  fun `verify a task with note is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), true, null, null, null, null)

    val tasks = taskRepository.findAllByProjectIdentifier(projectIdentifier, unpaged()).content

    tasks.apply {
      assertThat(this).hasSize(1)
      assertThat(this.first().name).isEqualTo("task1")
      assertThat(this.first().description).isEqualTo("This is a note.")
    }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "task1" }

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyNumberOfEvents(7)
  }

  // There doesn't seem to be a "notes" column in P6, therefore we don't test it.
  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "task-with-note-too-long.pp",
              "task-with-note-too-long.mpp",
              "task-with-note-too-long-ms.xml"])
  @ParameterizedTest
  fun `verify a task with too long note is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), false, null, null, null, null)

    val tasks = taskRepository.findAllByProjectIdentifier(projectIdentifier, unpaged()).content

    tasks.apply {
      assertThat(this).hasSize(1)
      assertThat(this.first().name).isEqualTo("task1")
      assertThat(this.first().description)
          .isEqualTo("This note is too long. ".repeat(44).take(MAX_DESCRIPTION_LENGTH))
    }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "task1" }

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyNumberOfEvents(7)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "empty-project.pp",
              "empty-project.mpp",
              "empty-project.xer",
              "empty-project-ms.xml",
              "empty-project-p6.xml"])
  @ParameterizedTest
  fun `verify an empty project can be processed but nothing is imported`(file: Resource) {
    projectImportService.import(project, readProject(file), false, null, null, null, null)

    projectEventStoreUtils.verifyEmpty()
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "task-with-status.pp",
              "task-with-status.mpp",
              "task-with-status.xer",
              "task-with-status-ms.xml",
              "task-with-status-p6.xml"])
  @ParameterizedTest
  fun `verify tasks with status are imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), true, null, null, null, null)

    val isPP = file.file.name.endsWith(".pp")
    val expectedCount = if (isPP) 4 else 3

    val tasks = taskRepository.findAllByProjectIdentifier(projectIdentifier, unpaged()).content
    assertThat(tasks).hasSize(expectedCount)

    val taskNotStarted = tasks.singleOrNull { it.name == "taskNotStarted" }
    assertThat(taskNotStarted).isNotNull
    assertThat(taskNotStarted?.status).isEqualTo(TaskStatusEnum.DRAFT)

    val taskStarted = tasks.singleOrNull { it.name == "taskStarted" }
    assertThat(taskStarted).isNotNull
    assertThat(taskStarted?.status).isEqualTo(TaskStatusEnum.STARTED)

    val taskFinished = tasks.singleOrNull { it.name == "taskFinished" }
    assertThat(taskFinished).isNotNull
    assertThat(taskFinished?.status).isEqualTo(TaskStatusEnum.ACCEPTED)

    projectEventStoreUtils.verifyContains(TaskEventAvro::class.java, CREATED, expectedCount, false)
    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, STARTED, 1, false)
        .map { it.aggregate.name }
        .all { it == "taskStarted" }
    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, ACCEPTED, 1, false)
        .map { it.aggregate.name }
        .all { it == "taskFinished" }

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 3, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, expectedCount, false)

    projectEventStoreUtils.verifyNumberOfEvents(if (isPP) 17 else 15)
  }
}
