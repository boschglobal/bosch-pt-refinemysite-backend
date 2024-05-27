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
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.test.FileSource
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectController
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import net.sf.mpxj.FieldType
import net.sf.mpxj.TaskField
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Pageable.unpaged

@EnableAllKafkaListeners
class ImportTaskWorkAreaIntegrationTest : AbstractImportIntegrationTest() {

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
              "task-without-workarea.pp",
              "task-without-workarea.mpp",
              "task-without-workarea.xer",
              "task-without-workarea-ms.xml",
              "task-without-workarea-p6.xml"])
  @ParameterizedTest
  fun `verify a task without workArea is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), false, null, null, null, null)

    val tasks = taskRepository.findAllByProjectIdentifier(projectIdentifier, unpaged()).content

    tasks.apply {
      assertThat(this).hasSize(1)
      assertThat(this.first().name).isEqualTo("task1")
      assertThat(this.first().workArea).isNull()
    }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 1, false)
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
              "task-with-workarea-in-column.pp",
              "task-with-workarea-in-column.mpp",
              "task-with-workarea-in-column.xer",
              "task-with-workarea-in-column-ms.xml",
              "task-with-workarea-in-column-p6.xml"])
  @ParameterizedTest
  fun `verify a task with workArea from column is imported successfully`(file: Resource) {
    projectImportService.import(
        project, readProject(file), false, null, null, "Discipline", "TEXT1")

    val tasks = taskRepository.findAllByProjectIdentifier(projectIdentifier, unpaged()).content

    tasks.apply {
      assertThat(this).hasSize(1)
      assertThat(this.filter { it.name.equals("task1") }).hasSize(1)
      assertThat(this.first { it.name.equals("task1") }.workArea?.name).isEqualTo("Discipline name")
    }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "task1" }

    projectEventStoreUtils
        .verifyContainsAndGet(
            WorkAreaEventAvro::class.java, WorkAreaEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "Discipline name" }

    projectEventStoreUtils.verifyContains(
        WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.CREATED, 0, false)

    projectEventStoreUtils.verifyContains(
        WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.ITEMADDED, 1, false)

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
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 2, false)

    projectEventStoreUtils.verifyNumberOfEvents(10)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "task-with-workarea-from-hierarchy.pp",
              "task-with-workarea-from-hierarchy.mpp",
              "task-with-workarea-from-hierarchy.xer",
              "task-with-workarea-from-hierarchy-ms.xml",
              "task-with-workarea-from-hierarchy-p6.xml"])
  @ParameterizedTest
  fun `verify a task with workArea from hierarchy is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), true, null, null, null, null)

    val tasks = taskRepository.findAllByProjectIdentifier(projectIdentifier, unpaged()).content

    tasks.apply {
      assertThat(this).hasSize(1)
      assertThat(this.first().name).isEqualTo("task1")
      assertThat(this.first().workArea?.name).isEqualTo("Level 3")
    }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "task1" }

    projectEventStoreUtils
        .verifyContainsAndGet(
            WorkAreaEventAvro::class.java, WorkAreaEventEnumAvro.CREATED, 3, false)
        .map { it.aggregate.name }
        .all { listOf("Level 1", "Level 2", "Level 3").contains(it) }

    projectEventStoreUtils.verifyContains(
        WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.CREATED, 0, false)

    projectEventStoreUtils.verifyContains(
        WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.ITEMADDED, 3, false)

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
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 4, false)

    projectEventStoreUtils.verifyNumberOfEvents(16)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "task-with-workarea-in-column-and-hierarchy.pp",
              "task-with-workarea-in-column-and-hierarchy.mpp",
              "task-with-workarea-in-column-and-hierarchy.xer",
              "task-with-workarea-in-column-and-hierarchy-ms.xml",
              "task-with-workarea-in-column-and-hierarchy-p6.xml"])
  @ParameterizedTest
  fun `verify a task with work area from column but given hierarchy is imported successfully`(
      file: Resource
  ) {
    val isPP = file.file.name.endsWith(".pp")
    projectImportService.import(
        project,
        readProject(file),
        false,
        null,
        null,
        if (isPP) "Working_Area" else "Working Area",
        "TEXT1")

    val tasks = taskRepository.findAllByProjectIdentifier(projectIdentifier, unpaged()).content

    tasks.apply {
      assertThat(this).hasSize(1)
      assertThat(this.first().workArea?.name).isEqualTo("Working Area from column")
    }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "task1" }

    projectEventStoreUtils
        .verifyContainsAndGet(
            WorkAreaEventAvro::class.java, WorkAreaEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "Work Area from column" }

    projectEventStoreUtils.verifyContains(
        WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.CREATED, 0, false)

    projectEventStoreUtils.verifyContains(
        WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.ITEMADDED, 1, false)

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
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 2, false)

    projectEventStoreUtils.verifyNumberOfEvents(10)
  }

  @FileSource(container = "project-import-testdata", files = ["task-with-resources.mpp"])
  @ParameterizedTest
  fun `verify a task with work area from resources is imported successfully Project`(
      file: Resource
  ) {
    val columnName = (TaskField.RESOURCE_NAMES as FieldType).name
    `verify a task with work area from resources is imported successfully`(file, columnName)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "task-with-resources.pp",
              "task-with-resources.xer",
              // Exception, as we can only import extra columns as custom fields from xml
              "task-with-resources-ms.xml",
              "task-with-resources-p6.xml"])
  @ParameterizedTest
  fun `verify a task with work area from resources is imported successfully P6`(file: Resource) {
    `verify a task with work area from resources is imported successfully`(file, "Resources")
  }

  private fun `verify a task with work area from resources is imported successfully`(
      file: Resource,
      column: String
  ) {
    val project = projectRepository.findOneByIdentifier(projectIdentifier)!!
    projectImportService.import(project, readProject(file), false, null, null, column, null)

    val isPP = file.file.name.endsWith(".pp")
    val expectedCount = if (isPP) 5 else 4

    val tasks =
        taskRepository
            .findAllByProjectIdentifier(projectIdentifier, Pageable.unpaged())
            .content
            .sortedBy { it.name }

    tasks.apply {
      assertThat(this).hasSize(expectedCount)
      assertThat(this[0].name).isEqualTo("Task1")
      assertThat(this[0].workArea?.name).isEqualTo("Mason")
      assertThat(this[1].name).isEqualTo("Task2")
      assertThat(this[1].workArea?.name).isEqualTo("Mason")
      assertThat(this[2].name).isEqualTo("Task3")
      assertThat(this[2].workArea?.name).isEqualTo("Carpenter")
      assertThat(this[3].name).isEqualTo("Task4")
      assertThat(this[3].workArea).isNull()
    }

    projectEventStoreUtils
        .verifyContains(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, expectedCount, false)
        .map { it.aggregate.name }
        .all { setOf("Task1", "Task2", "Task3", "Task4").contains(it) }

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 4, false)

    projectEventStoreUtils
        .verifyContains(WorkAreaEventAvro::class.java, WorkAreaListEventEnumAvro.CREATED, 2, false)
        .map { it.aggregate.name }
        .all { setOf("Mason", "Carpenter").contains(it) }

    projectEventStoreUtils.verifyContains(
        WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.CREATED, 0, false)

    projectEventStoreUtils.verifyContains(
        WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.ITEMADDED, 2, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, if (isPP) 7 else 6, false)

    projectEventStoreUtils.verifyContains(
        WorkdayConfigurationEventAvro::class.java,
        WorkdayConfigurationEventEnumAvro.UPDATED,
        1,
        false)

    projectEventStoreUtils.verifyNumberOfEvents(if (isPP) 25 else 23)
  }
}
