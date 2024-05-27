/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
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
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.test.FileSource
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectController
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository.TaskScheduleRepository
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.data.domain.Pageable.unpaged

@EnableAllKafkaListeners
class ImportTaskScheduleIntegrationTest : AbstractImportIntegrationTest() {

  @Autowired private lateinit var projectImportService: ProjectImportService

  @Autowired private lateinit var projectRepository: ProjectRepository

  @Autowired private lateinit var taskRepository: TaskRepository

  @Autowired private lateinit var taskScheduleRepository: TaskScheduleRepository

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
  fun `verify a task schedule imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), true, null, null, null, null)

    val tasks = taskRepository.findAllByProjectIdentifier(projectIdentifier, unpaged()).content
    assertThat(tasks).hasSize(1)

    taskScheduleRepository.findOneByTaskIdentifier(tasks.first().identifier).apply {
      assertThat(this).isNotNull
      assertThat(this!!.start).isEqualTo(LocalDate.of(2022, 3, 24))
      assertThat(this.end).isEqualTo(LocalDate.of(2022, 3, 25))
    }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskScheduleEventAvro::class.java, CREATED, 1, false)
        .map { it.aggregate }
        .forEach {
          assertThat(it.start).isEqualTo(LocalDate.of(2022, 3, 24).toEpochMilli())
          assertThat(it.end).isEqualTo(LocalDate.of(2022, 3, 25).toEpochMilli())
        }

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "task1" }

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
}
