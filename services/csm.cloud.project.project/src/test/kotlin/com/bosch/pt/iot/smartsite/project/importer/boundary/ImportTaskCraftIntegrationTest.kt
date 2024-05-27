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

@EnableAllKafkaListeners
class ImportTaskCraftIntegrationTest : AbstractImportIntegrationTest() {

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
              "task-without-craft.pp",
              "task-without-craft.mpp",
              "task-without-craft.xer",
              "task-without-craft-ms.xml",
              "task-without-craft-p6.xml"])
  @ParameterizedTest
  fun `verify a task without craft is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), true, null, null, null, null)

    val tasks =
        taskRepository.findAllByProjectIdentifier(projectIdentifier, Pageable.unpaged()).content

    tasks.apply {
      assertThat(this).hasSize(1)
      assertThat(this.first().name).isEqualTo("task1")
      assertThat(this.first().projectCraft.name).isEqualTo("RmS-Placeholder")
    }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "task1" }

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils
        .verifyContainsAndGet(
            ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "RmS-Placeholder" }

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
              "task-with-craft-from-column.pp",
              "task-with-craft-from-column.mpp",
              "task-with-craft-from-column.xer",
              "task-with-craft-from-column-ms.xml",
              "task-with-craft-from-column-p6.xml"])
  @ParameterizedTest
  fun `verify a task with craft from column is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), true, "Discipline", "TEXT1", null, null)

    val tasks =
        taskRepository.findAllByProjectIdentifier(projectIdentifier, Pageable.unpaged()).content

    tasks.apply {
      assertThat(this).hasSize(1)
      assertThat(this.first().name).isEqualTo("task1")
      assertThat(this.first().projectCraft.name).isEqualTo("Some craft")
    }

    projectEventStoreUtils
        .verifyContains(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "task1" }

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils
        .verifyContains(
            ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "Some craft" }

    projectEventStoreUtils.verifyContains(
        ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyNumberOfEvents(7)
  }

  @FileSource(container = "project-import-testdata", files = ["task-with-resources.mpp"])
  @ParameterizedTest
  fun `verify a task with craft from resources is imported successfully Project`(file: Resource) {
    val columnName = (TaskField.RESOURCE_NAMES as FieldType).name
    `verify a task with craft from resources is imported successfully`(file, columnName)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "task-with-resources.pp",
              "task-with-resources.xer",
              "task-with-resources-ms.xml",
              "task-with-resources-p6.xml"])
  @ParameterizedTest
  fun `verify a task with craft from resources is imported successfully P6`(file: Resource) {
    `verify a task with craft from resources is imported successfully`(file, "Resources")
  }

  private fun `verify a task with craft from resources is imported successfully`(
      file: Resource,
      column: String
  ) {
    projectImportService.import(project, readProject(file), false, column, null, null, null)

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
      assertThat(this[0].projectCraft.name).isEqualTo("Mason")
      assertThat(this[1].name).isEqualTo("Task2")
      assertThat(this[1].projectCraft.name).isEqualTo("Mason")
      assertThat(this[2].name).isEqualTo("Task3")
      assertThat(this[2].projectCraft.name).isEqualTo("Carpenter")
      assertThat(this[3].name).isEqualTo("Task4")
      assertThat(this[3].projectCraft.name).isEqualTo("RmS-Placeholder")
    }

    projectEventStoreUtils
        .verifyContains(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, expectedCount, false)
        .map { it.aggregate.name }
        .all { setOf("Task1", "Task2", "Task3", "Task4").contains(it) }

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 4, false)

    projectEventStoreUtils
        .verifyContains(
            ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 3, false)
        .map { it.aggregate.name }
        .all { setOf("Mason", "Carpenter", "RmS-Placeholder").contains(it) }

    projectEventStoreUtils.verifyContains(
        ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 3, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, expectedCount, false)

    projectEventStoreUtils.verifyContains(
        WorkdayConfigurationEventAvro::class.java,
        WorkdayConfigurationEventEnumAvro.UPDATED,
        1,
        false)

    projectEventStoreUtils.verifyNumberOfEvents(if (isPP) 23 else 21)
  }
}
