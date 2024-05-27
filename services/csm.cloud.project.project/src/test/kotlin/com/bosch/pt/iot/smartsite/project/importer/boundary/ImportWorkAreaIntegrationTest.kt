/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
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
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.test.FileSource
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectController
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea.Companion.MAX_WORKAREA_NAME_LENGTH
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource

@EnableAllKafkaListeners
class ImportWorkAreaIntegrationTest : AbstractImportIntegrationTest() {

  @Autowired private lateinit var projectImportService: ProjectImportService

  @Autowired private lateinit var projectRepository: ProjectRepository

  @Autowired private lateinit var workAreaRepository: WorkAreaRepository

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

  @Nested
  inner class `Given a work area column` {

    @FileSource(
        container = "project-import-testdata",
        files =
            [
                "workingArea-from-column.pp",
                "workingArea-from-column.mpp",
                "workingArea-from-column.xer",
                "workingArea-from-column-ms.xml",
                "workingArea-from-column-p6.xml"])
    @ParameterizedTest
    fun `verify a work area is imported successfully`(file: Resource) {
      val isPP = file.file.name.endsWith(".pp")
      projectImportService.import(
          project,
          readProject(file),
          false,
          null,
          null,
          if (isPP) "Working_Area" else "Working Area",
          "TEXT1")

      val workAreas =
          workAreaRepository.findAll().filter { it.project.identifier == projectIdentifier }

      workAreas.apply {
        assertThat(this).hasSize(1)
        assertThat(this.first().name).isEqualTo("workArea1")
        assertThat(this.first().position).isEqualTo(0)
      }

      projectEventStoreUtils
          .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 1, false)
          .map { it.aggregate.name }
          .all { it == "task01" }

      projectEventStoreUtils
          .verifyContainsAndGet(
              WorkAreaEventAvro::class.java, WorkAreaEventEnumAvro.CREATED, 1, false)
          .map { it.aggregate.name }
          .all { it == "workArea1" }

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

      projectEventStoreUtils.verifyContains(
          ProjectImportStartedEventAvro::class.java, null, 1, false)

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
                "workingArea-from-column-name-too-long.pp",
                "workingArea-from-column-name-too-long.mpp",
                "workingArea-from-column-name-too-long.xer",
                "workingArea-from-column-name-too-long-ms.xml",
                "workingArea-from-column-name-too-long-p6.xml"])
    @ParameterizedTest
    fun `verify a work area with too long name is imported successfully`(file: Resource) {
      val isPP = file.file.name.endsWith(".pp")
      projectImportService.import(
          project,
          readProject(file),
          false,
          null,
          null,
          if (isPP) "Working_Area" else "Working Area",
          "TEXT1")

      val workAreas =
          workAreaRepository.findAll().filter { it.project.identifier == projectIdentifier }

      workAreas.apply {
        assertThat(this).hasSize(1)
        assertThat(this.first().name)
            .isEqualTo("Work area name too long. ".repeat(5).take(MAX_WORKAREA_NAME_LENGTH))
        assertThat(this.first().position).isEqualTo(0)
      }

      projectEventStoreUtils
          .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 1, false)
          .map { it.aggregate.name }
          .all { it == "task01" }

      projectEventStoreUtils
          .verifyContainsAndGet(
              WorkAreaEventAvro::class.java, WorkAreaEventEnumAvro.CREATED, 1, false)
          .map { it.aggregate.name }
          .all { it.startsWith("Work area name too long.") }

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

      projectEventStoreUtils.verifyContains(
          ProjectImportStartedEventAvro::class.java, null, 1, false)

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
                "workingArea-1001-from-column.pp",
                "workingArea-1001-from-column.mpp",
                "workingArea-1001-from-column.xer",
                "workingArea-1001-from-column-ms.xml",
                "workingArea-1001-from-column-p6.xml",
            ])
    @ParameterizedTest
    fun `verify 1001 work areas are not imported`(file: Resource) {
      val isPP = file.file.name.endsWith(".pp")
      assertThrows<PreconditionViolationException> {
        projectImportService.import(
            project,
            readProject(file),
            false,
            null,
            null,
            if (isPP) "Working_Area" else "Working Area",
            "TEXT1")
      }

      val workAreas =
          workAreaRepository.findAll().filter { it.project.identifier == projectIdentifier }

      assertThat(workAreas).isEmpty()

      projectEventStoreUtils.verifyEmpty()
    }
  }

  @Nested
  inner class `Given a work area hierarchy` {

    // TODO: Decide what to do with this test as shortening is no longer applied
    @Disabled
    @FileSource(
        container = "project-import-testdata",
        files =
            [
                "workingArea-with-duplicates-after-shortening.pp",
                "workingArea-with-duplicates-after-shortening.mpp",
                "workingArea-with-duplicates-after-shortening.xer",
                "workingArea-with-duplicates-after-shortening-ms.xml",
                "workingArea-with-duplicates-after-shortening-p6.xml"])
    @ParameterizedTest
    fun `verify duplicates after shortening are prefixed with characters`(file: Resource) {
      projectImportService.import(project, readProject(file), true, null, null, null, null)

      val workAreas =
          workAreaRepository.findAll().filter { it.project.identifier == projectIdentifier }

      workAreas.apply {
        assertThat(this).hasSize(2)
        assertThat(this[0].name)
            .isEqualTo(
                "A P/B/W/Work A/Work Area 3/Work Area 4/Work Area 5/" +
                    "Work Area 6/Work Area 7/Work Area 8/Work Area 9")
        assertThat(this[1].name)
            .isEqualTo(
                "B P/B/W/Work A/Work Area 3/Work Area 4/Work Area 5/" +
                    "Work Area 6/Work Area 7/Work Area 8/Work Area 9")
      }

      projectEventStoreUtils
          .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 2, false)
          .map { it.aggregate.name }
          .all { it == "task" }

      projectEventStoreUtils
          .verifyContainsAndGet(
              WorkAreaEventAvro::class.java, WorkAreaEventEnumAvro.CREATED, 2, false)
          .map { it.aggregate.name }
          .all {
            listOf(
                    "A P/B/W/Work A/Work Area 3/Work Area 4/Work Area 5/" +
                        "Work Area 6/Work Area 7/Work Area 8/Work Area 9",
                    "B P/B/W/Work A/Work Area 3/Work Area 4/Work Area 5/" +
                        "Work Area 6/Work Area 7/Work Area 8/Work Area 9")
                .contains(it)
          }

      projectEventStoreUtils.verifyContains(
          WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.CREATED, 0, false)

      projectEventStoreUtils.verifyContains(
          WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.ITEMADDED, 2, false)

      projectEventStoreUtils.verifyContains(
          TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 2, false)

      projectEventStoreUtils.verifyContains(
          ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)

      projectEventStoreUtils.verifyContains(
          ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 1, false)

      projectEventStoreUtils.verifyContains(
          ProjectImportStartedEventAvro::class.java, null, 1, false)

      projectEventStoreUtils.verifyContains(
          ProjectImportFinishedEventAvro::class.java, null, 1, false)

      projectEventStoreUtils.verifyContains(
          ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 4, false)

      projectEventStoreUtils.verifyNumberOfEvents(16)
    }

    // TODO: Replace test files with longer names and move to warning integration test
    @Disabled
    @FileSource(
        container = "project-import-testdata",
        files =
            [
                "workingArea-from-2-level-hierarchy.pp",
                "workingArea-from-2-level-hierarchy.mpp",
                "workingArea-from-2-level-hierarchy.xer",
                "workingArea-from-2-level-hierarchy-ms.xml",
                "workingArea-from-2-level-hierarchy-p6.xml"])
    @ParameterizedTest
    fun `verify a 2 level hierarchy with 1 too long name is imported successfully`(file: Resource) {
      projectImportService.import(project, readProject(file), true, null, null, null, null)

      val workAreas =
          workAreaRepository.findAll().filter { it.project.identifier == projectIdentifier }

      workAreas.apply {
        assertThat(this).hasSize(1)
        assertThat(this.first().name)
            .isEqualTo(
                "lev/level2level2level2level2level2level2level2level2level2level2level2level2level2level2level2level2")
        assertThat(this.first().name.length).isLessThanOrEqualTo(100)
      }

      projectEventStoreUtils
          .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 1, false)
          .map { it.aggregate.name }
          .all { it == "task1" }

      projectEventStoreUtils
          .verifyContainsAndGet(
              WorkAreaEventAvro::class.java, WorkAreaEventEnumAvro.CREATED, 1, false)
          .map { it.aggregate.name }
          .all {
            it ==
                "lev/level2level2level2level2level2level2level2level2" +
                    "level2level2level2level2level2level2level2level2"
          }

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

      projectEventStoreUtils.verifyContains(
          ProjectImportStartedEventAvro::class.java, null, 1, false)

      projectEventStoreUtils.verifyContains(
          ProjectImportFinishedEventAvro::class.java, null, 1, false)

      projectEventStoreUtils.verifyContains(
          ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 2, false)

      projectEventStoreUtils.verifyNumberOfEvents(10)
    }

    // TODO: Replace test files with longer names and move to warning integration test
    @Disabled
    @FileSource(
        container = "project-import-testdata",
        files =
            [
                "workingArea-from-3-level-hierarchy-with-names-too-long.pp",
                "workingArea-from-3-level-hierarchy-with-names-too-long.mpp",
                "workingArea-from-3-level-hierarchy-with-names-too-long.xer",
                "workingArea-from-3-level-hierarchy-with-names-too-long-ms.xml",
                "workingArea-from-3-level-hierarchy-with-names-too-long-p6.xml"])
    @ParameterizedTest
    fun `verify a 3 level hierarchy with 2 too long names is imported successfully`(
        file: Resource
    ) {
      projectImportService.import(project, readProject(file), true, null, null, null, null)

      val workAreas =
          workAreaRepository.findAll().filter { it.project.identifier == projectIdentifier }

      workAreas.apply {
        assertThat(this).hasSize(1)
        assertThat(this.first().name)
            .isEqualTo(
                "l/l/level3level3level3level3level3level3level3level3" +
                    "level3level3level3level3level3level3level3le d1f")
        assertThat(this.first().name.length).isLessThanOrEqualTo(100)
      }

      projectEventStoreUtils
          .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 1, false)
          .map { it.aggregate.name }
          .all { it == "task1" }

      projectEventStoreUtils
          .verifyContainsAndGet(
              WorkAreaEventAvro::class.java, WorkAreaEventEnumAvro.CREATED, 1, false)
          .map { it.aggregate.name }
          .all {
            it ==
                "l/l/level3level3level3level3level3level3level3level3" +
                    "level3level3level3level3level3level3level3le d1f"
          }

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

      projectEventStoreUtils.verifyContains(
          ProjectImportStartedEventAvro::class.java, null, 1, false)

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
                "workingArea-from-3-level-hierarchy-tasks-in-every-level.pp",
                "workingArea-from-3-level-hierarchy-tasks-in-every-level.mpp",
                "workingArea-from-3-level-hierarchy-tasks-in-every-level.xer",
                "workingArea-from-3-level-hierarchy-tasks-in-every-level-ms.xml",
                "workingArea-from-3-level-hierarchy-tasks-in-every-level-p6.xml"])
    @ParameterizedTest
    fun `verify a 3 level hierarchy with tasks in every level is imported successfully`(
        file: Resource
    ) {
      projectImportService.import(project, readProject(file), true, null, null, null, null)

      val workAreas =
          workAreaRepository.findAll().filter { it.project.identifier == projectIdentifier }

      workAreas.apply {
        assertThat(this).hasSize(3)
        assertThat(this[0].name).isEqualTo("level 1")
        assertThat(this[0].parent).isNull()
        assertThat(this[1].name).isEqualTo("level 2")
        assertThat(this[1].parent).isEqualTo(this[0].identifier)
        assertThat(this[2].name).isEqualTo("level 3")
        assertThat(this[2].parent).isEqualTo(this[1].identifier)
      }

      projectEventStoreUtils
          .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 5, false)
          .map { it.aggregate.name }
          .all { listOf("task0", "task 1a", "task 1b", "task2", "task3").contains(it) }

      projectEventStoreUtils
          .verifyContainsAndGet(
              WorkAreaEventAvro::class.java, WorkAreaEventEnumAvro.CREATED, 3, false)
          .map { it.aggregate.name }
          .all { listOf("level 1", "level 2", "level 3").contains(it) }

      projectEventStoreUtils.verifyContains(
          WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.CREATED, 0, false)

      projectEventStoreUtils.verifyContains(
          WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.ITEMADDED, 3, false)

      projectEventStoreUtils.verifyContains(
          TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 5, false)

      projectEventStoreUtils.verifyContains(
          ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)

      projectEventStoreUtils.verifyContains(
          ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 1, false)

      projectEventStoreUtils.verifyContains(
          ProjectImportStartedEventAvro::class.java, null, 1, false)

      projectEventStoreUtils.verifyContains(
          ProjectImportFinishedEventAvro::class.java, null, 1, false)

      projectEventStoreUtils.verifyContains(
          ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 8, false)

      projectEventStoreUtils.verifyNumberOfEvents(28)
    }

    @FileSource(
        container = "project-import-testdata",
        files =
            [
                "workingArea-from-3-level-hierarchy-tasks-in-some-levels.pp",
                "workingArea-from-3-level-hierarchy-tasks-in-some-levels.mpp",
                "workingArea-from-3-level-hierarchy-tasks-in-some-levels.xer",
                "workingArea-from-3-level-hierarchy-tasks-in-some-levels-ms.xml",
                "workingArea-from-3-level-hierarchy-tasks-in-some-levels-p6.xml"])
    @ParameterizedTest
    fun `verify a 3 level hierarchy with tasks in only some levels is imported successfully`(
        file: Resource
    ) {
      projectImportService.import(project, readProject(file), true, null, null, null, null)

      val workAreas =
          workAreaRepository.findAll().filter { it.project.identifier == projectIdentifier }

      workAreas.apply {
        assertThat(this).hasSize(3)
        assertThat(this[0].name).isEqualTo("level 1")
        assertThat(this[0].parent).isNull()
        assertThat(this[1].name).isEqualTo("level 2")
        assertThat(this[1].parent).isEqualTo(this[0].identifier)
        assertThat(this[2].name).isEqualTo("level 3")
        assertThat(this[2].parent).isEqualTo(this[1].identifier)
      }

      projectEventStoreUtils
          .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 3, false)
          .map { it.aggregate.name }
          .all { listOf("task 1a", "task 1b", "task3").contains(it) }

      projectEventStoreUtils
          .verifyContainsAndGet(
              WorkAreaEventAvro::class.java, WorkAreaEventEnumAvro.CREATED, 3, false)
          .map { it.aggregate.name }
          .all { listOf("level 1", "level 2", "level 3").contains(it) }

      projectEventStoreUtils.verifyContains(
          WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.CREATED, 0, false)

      projectEventStoreUtils.verifyContains(
          WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.ITEMADDED, 3, false)

      projectEventStoreUtils.verifyContains(
          TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 3, false)

      projectEventStoreUtils.verifyContains(
          ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)

      projectEventStoreUtils.verifyContains(
          ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 1, false)

      projectEventStoreUtils.verifyContains(
          ProjectImportStartedEventAvro::class.java, null, 1, false)

      projectEventStoreUtils.verifyContains(
          ProjectImportFinishedEventAvro::class.java, null, 1, false)

      projectEventStoreUtils.verifyContains(
          ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 6, false)

      projectEventStoreUtils.verifyNumberOfEvents(22)
    }

    @FileSource(
        container = "project-import-testdata",
        files =
            [
                "workingArea-from-3-level-hierarchy-with-empty-name.pp",
                "workingArea-from-3-level-hierarchy-with-empty-name.mpp",
                "workingArea-from-3-level-hierarchy-with-empty-name.xer",
                "workingArea-from-3-level-hierarchy-with-empty-name-ms.xml",
                "workingArea-from-3-level-hierarchy-with-empty-name-p6.xml"])
    @ParameterizedTest
    fun `verify a 3 level hierarchy with empty name is imported successfully`(file: Resource) {
      projectImportService.import(project, readProject(file), true, null, null, null, null)

      val workAreas =
          workAreaRepository.findAll().filter { it.project.identifier == projectIdentifier }

      workAreas.apply {
        assertThat(this).hasSize(3)
        assertThat(this[0].name).isEqualTo("Placeholder1")
        assertThat(this[1].name).isEqualTo("level 1")
        assertThat(this[2].name).isEqualTo("level 3")

        assertThat(this[1].parent).isNull()
        assertThat(this[0].parent).isEqualTo(this[1].identifier)
        assertThat(this[2].parent).isEqualTo(this[0].identifier)
      }

      projectEventStoreUtils
          .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 1, false)
          .map { it.aggregate.name }
          .all { it == "task 1" }

      projectEventStoreUtils
          .verifyContainsAndGet(
              WorkAreaEventAvro::class.java, WorkAreaEventEnumAvro.CREATED, 3, false)
          .map { it.aggregate.name }
          .all { listOf("level 1", "Placeholder1", "level 3").contains(it) }

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

      projectEventStoreUtils.verifyContains(
          ProjectImportStartedEventAvro::class.java, null, 1, false)

      projectEventStoreUtils.verifyContains(
          ProjectImportFinishedEventAvro::class.java, null, 1, false)

      projectEventStoreUtils.verifyContains(
          ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 4, false)

      projectEventStoreUtils.verifyNumberOfEvents(16)
    }
  }
}
