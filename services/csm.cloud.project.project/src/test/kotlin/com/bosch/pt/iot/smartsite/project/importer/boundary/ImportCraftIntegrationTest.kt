/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary

import com.bosch.pt.csm.cloud.common.extensions.toUUID
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
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.test.FileSource
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectController
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft.Companion.MAX_NAME_LENGTH
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftListRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource

@EnableAllKafkaListeners
class ImportCraftIntegrationTest : AbstractImportIntegrationTest() {

  @Autowired private lateinit var projectImportService: ProjectImportService

  @Autowired private lateinit var projectRepository: ProjectRepository

  @Autowired private lateinit var projectCraftListRepository: ProjectCraftListRepository

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
              "craft-tasks-with-craft.pp",
              "craft-tasks-with-craft.mpp",
              "craft-tasks-with-craft.xer",
              "craft-tasks-with-craft-ms.xml",
              "craft-tasks-with-craft-p6.xml"])
  @ParameterizedTest
  fun `verify a craft is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), true, "Discipline", "TEXT1", null, null)

    val crafts =
        projectCraftListRepository
            .findOneWithDetailsByProjectIdentifier(projectIdentifier)!!
            .projectCrafts

    crafts.apply {
      assertThat(this).hasSize(2)
      assertThat(this[0].name).isEqualTo("craft 1")
      assertThat(this[0].color).isEqualTo("#f5a100")
      assertThat(this[1].name).isEqualTo("RmS-Placeholder")
      assertThat(this[1].color).isEqualTo("#d9c200")
    }

    projectEventStoreUtils
        .verifyContainsAndGet(
            ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 2, false)
        .map { it.aggregate.name }
        .all { listOf("craft 1", "Placeholder").contains(it) }

    projectEventStoreUtils.verifyContains(
        ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 2, false)

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 3, false)
        .map { it.aggregate.name }
        .all { it.startsWith("task ") }

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 3, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 3, false)

    projectEventStoreUtils.verifyNumberOfEvents(15)
  }

  @FileSource(
      container = "project-import-testdata",
      files = ["craft-tasks-with-craft.mpp", "craft-tasks-with-craft-ms.xml"])
  @ParameterizedTest
  fun `verify ids if a craft is imported successfully ms project`(file: Resource) {
    val projectFile = readProject(file)
    projectImportService.import(project, projectFile, true, "Discipline", null, null, null)

    val importModel = readFile(projectFile, project, "Discipline")
    assertThat(importModel.crafts).hasSize(2)
    assertThat(importModel.tasks).hasSize(3)
    assertThat(importModel.taskSchedules).hasSize(3)

    val importIds = getImportIds(project, projectFile)
    assertThat(importIds).hasSize(3)

    val task1ImportId = importIds.single { it.guid == importModel.tasks[0].guid }
    assertThat(task1ImportId.guid).isEqualTo("90A9588C-64C5-EC11-B757-001C427ADB10".toUUID())
    assertThat(task1ImportId.fileUniqueId).isEqualTo(1)
    assertThat(task1ImportId.fileId).isEqualTo(1)

    val task2ImportId = importIds.single { it.guid == importModel.tasks[1].guid }
    assertThat(task2ImportId.guid).isEqualTo("081AE5A0-64C5-EC11-B757-001C427ADB10".toUUID())
    assertThat(task2ImportId.fileUniqueId).isEqualTo(2)
    assertThat(task2ImportId.fileId).isEqualTo(2)

    val task3ImportId = importIds.single { it.guid == importModel.tasks[2].guid }
    assertThat(task3ImportId.guid).isEqualTo("83D379A9-64C5-EC11-B757-001C427ADB10".toUUID())
    assertThat(task3ImportId.fileUniqueId).isEqualTo(3)
    assertThat(task3ImportId.fileId).isEqualTo(3)
  }

  @FileSource(container = "project-import-testdata", files = ["craft-tasks-with-craft.xer"])
  @ParameterizedTest
  fun `verify ids if a craft is imported successfully p6 xer`(file: Resource) {
    val projectFile = readProject(file)
    projectImportService.import(project, projectFile, true, "Discipline", null, null, null)

    val importModel = readFile(projectFile, project, "Discipline")
    assertThat(importModel.crafts).hasSize(2)
    assertThat(importModel.tasks).hasSize(3)
    assertThat(importModel.taskSchedules).hasSize(3)

    val importIds = getImportIds(project, projectFile)
    assertThat(importIds).hasSize(3)

    // The guid is null in file and (re-)reading the file creates new identifiers.
    // Find the import ID by unique ID instead (not as precise but shows modifications).
    val task1ImportId =
        importIds.single {
          it.fileUniqueId == importModel.tasks.single { it.name == "task 1 with craft 1" }.uniqueId
        }
    assertThat(task1ImportId.guid).isNotNull
    assertThat(task1ImportId.fileUniqueId).isEqualTo(35995)
    assertThat(task1ImportId.fileId).isEqualTo(2)

    val task2ImportId =
        importIds.single {
          it.fileUniqueId == importModel.tasks.single { it.name == "task 2 without craft" }.uniqueId
        }
    assertThat(task2ImportId.guid).isNotNull
    assertThat(task2ImportId.fileUniqueId).isEqualTo(35996)
    assertThat(task2ImportId.fileId).isEqualTo(3)

    val task3ImportId =
        importIds.single {
          it.fileUniqueId == importModel.tasks.single { it.name == "task 3 with craft 1" }.uniqueId
        }
    assertThat(task3ImportId.guid).isNotNull
    assertThat(task3ImportId.fileUniqueId).isEqualTo(35997)
    assertThat(task3ImportId.fileId).isEqualTo(4)
  }

  // TODO Try to re-enable after v12. Update. Toggles repetitive without changes in the code.
  @Disabled
  @FileSource(container = "project-import-testdata", files = ["craft-tasks-with-craft.pp"])
  @ParameterizedTest
  fun `verify ids if a craft is imported successfully pp`(file: Resource) {
    val projectFile = readProject(file)
    projectImportService.import(project, projectFile, true, "Discipline", "TEXT1", null, null)

    val importModel = readFile(projectFile, project, "Discipline")
    assertThat(importModel.crafts).hasSize(2)
    assertThat(importModel.tasks).hasSize(3)
    assertThat(importModel.taskSchedules).hasSize(3)

    val importIds = getImportIds(project, projectFile)
    assertThat(importIds).hasSize(3)

    // The guid is null in file and (re-)reading the file creates new identifiers.
    // Find the import ID by unique ID instead (not as precise but shows modifications).
    val task1ImportId =
        importIds.single {
          it.fileUniqueId == importModel.tasks.single { it.name == "task 1 with craft 1" }.uniqueId
        }
    assertThat(task1ImportId.guid).isNotNull
    assertThat(task1ImportId.fileUniqueId).isEqualTo(1001)
    assertThat(task1ImportId.fileId).isEqualTo(1)

    val task2ImportId =
        importIds.single {
          it.fileUniqueId == importModel.tasks.single { it.name == "task 2 without craft" }.uniqueId
        }
    assertThat(task2ImportId.guid).isNotNull
    assertThat(task2ImportId.fileUniqueId).isEqualTo(1003)
    assertThat(task2ImportId.fileId).isEqualTo(2)

    val task3ImportId =
        importIds.single {
          it.fileUniqueId == importModel.tasks.single { it.name == "task 3 with craft 1" }.uniqueId
        }
    assertThat(task3ImportId.guid).isNotNull
    assertThat(task3ImportId.fileUniqueId).isEqualTo(1005)
    assertThat(task3ImportId.fileId).isEqualTo(3)
  }

  @FileSource(container = "project-import-testdata", files = ["craft-tasks-with-craft-p6.xml"])
  @ParameterizedTest
  fun `verify ids if a craft is imported successfully p6 xml`(file: Resource) {
    val projectFile = readProject(file)
    projectImportService.import(project, projectFile, true, "Discipline", null, null, null)

    val importModel = readFile(projectFile, project, "Discipline")
    assertThat(importModel.crafts).hasSize(2)
    assertThat(importModel.tasks).hasSize(3)
    assertThat(importModel.taskSchedules).hasSize(3)

    val importIds = getImportIds(project, projectFile)
    assertThat(importIds).hasSize(3)

    // The guid is null in file and (re-)reading the file creates new identifiers.
    // Find the import ID by unique ID instead (not as precise but shows modifications).
    val task1ImportId =
        importIds.single {
          it.fileUniqueId == importModel.tasks.single { it.name == "task 1 with craft 1" }.uniqueId
        }
    assertThat(task1ImportId.guid).isNotNull
    assertThat(task1ImportId.fileUniqueId).isEqualTo(35995)
    assertThat(task1ImportId.fileId).isEqualTo(1)

    val task2ImportId =
        importIds.single {
          it.fileUniqueId == importModel.tasks.single { it.name == "task 2 without craft" }.uniqueId
        }
    assertThat(task2ImportId.guid).isNotNull
    assertThat(task2ImportId.fileUniqueId).isEqualTo(35996)
    assertThat(task2ImportId.fileId).isEqualTo(2)

    val task3ImportId =
        importIds.single {
          it.fileUniqueId == importModel.tasks.single { it.name == "task 3 with craft 1" }.uniqueId
        }
    assertThat(task3ImportId.guid).isNotNull
    assertThat(task3ImportId.fileUniqueId).isEqualTo(35997)
    assertThat(task3ImportId.fileId).isEqualTo(3)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "craft-41-different-crafts.pp",
              "craft-41-different-crafts.mpp",
              "craft-41-different-crafts.xer",
              "craft-41-different-crafts-ms.xml",
              "craft-41-different-crafts-p6.xml"])
  @ParameterizedTest
  fun `verify 41 crafts are imported successfully`(file: Resource) {
    projectImportService.import(
        project, readProject(file), false, "Discipline", "TEXT1", null, null)

    val crafts =
        projectCraftListRepository
            .findOneWithDetailsByProjectIdentifier(projectIdentifier)!!
            .projectCrafts
    crafts.apply {
      assertThat(this).hasSize(41)
      assertThat(this.first().color).isEqualTo(this.last().color)
      assertThat(this.distinctBy { it.color }).hasSize(40)
    }

    projectEventStoreUtils
        .verifyContainsAndGet(
            ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 41, false)
        .map { it.aggregate.name }
        .all { it.startsWith("craft ") }

    projectEventStoreUtils.verifyContains(
        ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 41, false)

    projectEventStoreUtils
        .verifyContains(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 41, false)
        .map { it.aggregate.name }
        .all { it.startsWith("task ") }

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 41, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 41, false)

    projectEventStoreUtils.verifyNumberOfEvents(207)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "craft-name-too-long.pp",
              "craft-name-too-long.mpp",
              "craft-name-too-long.xer",
              "craft-name-too-long-ms.xml",
              "craft-name-too-long-p6.xml"])
  @ParameterizedTest
  fun `verify a craft with too long name is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), true, "Discipline", "TEXT1", null, null)

    val crafts =
        projectCraftListRepository
            .findOneWithDetailsByProjectIdentifier(projectIdentifier)!!
            .projectCrafts

    crafts.apply {
      assertThat(this).hasSize(1)
      assertThat(this.first().name)
          .isEqualTo("Craft name is too long. ".repeat(5).take(MAX_NAME_LENGTH))
    }

    projectEventStoreUtils
        .verifyContainsAndGet(
            ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it.startsWith("Craft ") }

    projectEventStoreUtils.verifyContains(
        ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 1, false)

    projectEventStoreUtils
        .verifyContains(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "task01" }

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 1, false)

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
              "craft-names-mixed-case.pp",
              "craft-names-mixed-case.mpp",
              "craft-names-mixed-case.xer",
              "craft-names-mixed-case-ms.xml",
              "craft-names-mixed-case-p6.xml"])
  @ParameterizedTest
  fun `verify crafts with mixed case are imported deduplicated`(file: Resource) {
    projectImportService.import(project, readProject(file), true, "Discipline", "TEXT1", null, null)

    val crafts =
        projectCraftListRepository
            .findOneWithDetailsByProjectIdentifier(projectIdentifier)!!
            .projectCrafts

    crafts.apply {
      assertThat(this).hasSize(1)
      assertThat(this.first().name).isEqualToIgnoringCase("Craft1")
    }

    projectEventStoreUtils
        .verifyContainsAndGet(
            ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { "Craft1".equals(it, ignoreCase = true) }

    projectEventStoreUtils.verifyContains(
        ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 1, false)

    projectEventStoreUtils
        .verifyContains(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 2, false)
        .map { it.aggregate.name }
        .all { listOf("task1", "task2").contains(it) }

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 2, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 2, false)

    projectEventStoreUtils.verifyNumberOfEvents(10)
  }
}
