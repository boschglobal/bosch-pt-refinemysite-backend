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
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.test.FileSource
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone.Companion.MAX_DESCRIPTION_LENGTH
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectController
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource

@EnableAllKafkaListeners
class ImportMilestoneIntegrationTest : AbstractImportIntegrationTest() {

  @Autowired private lateinit var projectImportService: ProjectImportService

  @Autowired private lateinit var projectRepository: ProjectRepository

  @Autowired private lateinit var milestoneRepository: MilestoneRepository

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
              "milestone.pp",
              "milestone.mpp",
              "milestone.xer",
              "milestone-ms.xml",
              "milestone-p6.xml"])
  @ParameterizedTest
  fun `verify a header milestone is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), false, null, null, null, null)

    val milestones = milestoneRepository.findAllByProjectIdentifier(projectIdentifier)

    milestones.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].name).isEqualTo("milestone1")
      assertThat(this[0].date).isEqualTo(LocalDate.of(2022, 4, 29))
      assertThat(this[0].header).isTrue
      assertThat(this[0].type).isEqualTo(MilestoneTypeEnum.PROJECT)
    }

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "milestone1" }

    projectEventStoreUtils.verifyContainsAndGet(
        MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyNumberOfEvents(5)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "milestone-name-too-long.pp",
              "milestone-name-too-long.mpp",
              "milestone-name-too-long.xer",
              "milestone-name-too-long-ms.xml",
              "milestone-name-too-long-p6.xml"])
  @ParameterizedTest
  fun `verify a milestone with too long name is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), false, null, null, null, null)

    val milestones = milestoneRepository.findAllByProjectIdentifier(projectIdentifier)

    milestones.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].name)
          .isEqualTo(
              "milestone1milestone1milestone1milestone1milestone1milestone1milestone1milestone1milestone1milestone1")
      assertThat(this[0].date).isEqualTo(LocalDate.of(2022, 4, 29))
      assertThat(this[0].header).isTrue
      assertThat(this[0].type).isEqualTo(MilestoneTypeEnum.PROJECT)
    }

    projectEventStoreUtils
        .verifyContains(MilestoneEventAvro::class.java, MilestoneEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it.startsWith("milestone1") }

    projectEventStoreUtils.verifyContainsAndGet(
        MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyNumberOfEvents(5)
  }

  // In P6 you cannot create milestones without names
  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "milestone-without-name.pp",
              "milestone-without-name.mpp",
              "milestone-without-name-ms.xml"])
  @ParameterizedTest
  fun `verify a milestone without name is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), false, null, null, null, null)

    val milestones = milestoneRepository.findAllByProjectIdentifier(projectIdentifier)

    milestones.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].name).isEqualTo("Unnamed Milestone")
      assertThat(this[0].date).isEqualTo(LocalDate.of(2022, 4, 29))
      assertThat(this[0].header).isTrue
      assertThat(this[0].type).isEqualTo(MilestoneTypeEnum.PROJECT)
    }

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "Unnamed Milestone" }

    projectEventStoreUtils.verifyContainsAndGet(
        MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyNumberOfEvents(5)
  }

  // There doesn't seem to be a "notes" column in P6 and PP, therefore we don't test it.
  @FileSource(
      container = "project-import-testdata",
      files = ["milestone-notes-too-long.mpp", "milestone-notes-too-long-ms.xml"])
  @ParameterizedTest
  fun `verify a milestone with too long notes is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), false, null, null, null, null)

    val milestones = milestoneRepository.findAllByProjectIdentifier(projectIdentifier)

    milestones.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].name).isEqualTo("milestone1")
      assertThat(this[0].date).isEqualTo(LocalDate.of(2022, 6, 1))
      assertThat(this[0].header).isTrue
      assertThat(this[0].type).isEqualTo(MilestoneTypeEnum.PROJECT)
      assertThat(this[0].description).startsWith("Milestone notes too long.")
      assertThat(this[0].description?.length).isEqualTo(MAX_DESCRIPTION_LENGTH)
    }

    projectEventStoreUtils
        .verifyContains(MilestoneEventAvro::class.java, MilestoneEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it.equals("milestone1") }

    projectEventStoreUtils.verifyContainsAndGet(
        MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyNumberOfEvents(5)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "milestone-with-craft.pp",
              "milestone-with-craft.mpp",
              "milestone-with-craft.xer",
              "milestone-with-craft-ms.xml",
              "milestone-with-craft-p6.xml"])
  @ParameterizedTest
  fun `verify a milestone with craft is imported successfully`(file: Resource) {
    projectImportService.import(
        project, readProject(file), false, "Discipline", "TEXT2", null, null)

    val milestones = milestoneRepository.findAllByProjectIdentifier(projectIdentifier)

    milestones.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].name).isEqualTo("milestone1")
      assertThat(this[0].date).isEqualTo(LocalDate.of(2022, 4, 29))
      assertThat(this[0].header).isTrue
      assertThat(this[0].type).isEqualTo(MilestoneTypeEnum.CRAFT)
    }

    projectEventStoreUtils
        .verifyContains(MilestoneEventAvro::class.java, MilestoneEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "milestone1" }

    projectEventStoreUtils.verifyContains(
        MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils
        .verifyContains(
            ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "craft name 1" }

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
              "milestone-with-workarea-from-column.pp",
              "milestone-with-workarea-from-column.mpp",
              "milestone-with-workarea-from-column.xer",
              "milestone-with-workarea-from-column-ms.xml",
              "milestone-with-workarea-from-column-p6.xml"])
  @ParameterizedTest
  fun `verify a milestone with workarea from column is imported successfully`(file: Resource) {
    val isPP = file.file.name.endsWith(".pp")
    projectImportService.import(
        project,
        readProject(file),
        false,
        null,
        null,
        if (isPP) "Working_Area" else "Working Area",
        "TEXT2")

    val milestones = milestoneRepository.findAllByProjectIdentifier(projectIdentifier)

    milestones.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].name).isEqualTo("milestone1")
      assertThat(this[0].date).isEqualTo(LocalDate.of(2022, 4, 29))
      assertThat(this[0].header).isFalse
      assertThat(this[0].type).isEqualTo(MilestoneTypeEnum.PROJECT)
    }

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "milestone1" }

    projectEventStoreUtils.verifyContains(
        MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils
        .verifyContainsAndGet(
            WorkAreaEventAvro::class.java, WorkAreaEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "work area name 1" }

    projectEventStoreUtils.verifyContains(
        WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.CREATED, 0, false)

    projectEventStoreUtils.verifyContains(
        WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.ITEMADDED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 2, false)

    projectEventStoreUtils.verifyNumberOfEvents(8)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "milestone-with-workarea-from-hierarchy.pp",
              "milestone-with-workarea-from-hierarchy.mpp",
              "milestone-with-workarea-from-hierarchy.xer",
              "milestone-with-workarea-from-hierarchy-ms.xml",
              "milestone-with-workarea-from-hierarchy-p6.xml"])
  @ParameterizedTest
  fun `verify a milestone with workarea from hierarchy is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), true, null, null, null, null)

    val milestones = milestoneRepository.findAllByProjectIdentifier(projectIdentifier)

    milestones.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].name).isEqualTo("milestone1")
      assertThat(this[0].date).isEqualTo(LocalDate.of(2022, 4, 29))
      assertThat(this[0].header).isFalse
      assertThat(this[0].type).isEqualTo(MilestoneTypeEnum.PROJECT)
    }

    projectEventStoreUtils
        .verifyContains(MilestoneEventAvro::class.java, MilestoneEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "milestone1" }

    projectEventStoreUtils.verifyContains(
        MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils
        .verifyContains(WorkAreaEventAvro::class.java, WorkAreaEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "Working Area 1" }

    projectEventStoreUtils.verifyContains(
        WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.CREATED, 0, false)

    projectEventStoreUtils.verifyContains(
        WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.ITEMADDED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 2, false)

    projectEventStoreUtils.verifyNumberOfEvents(8)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "milestone-task-with-duration-0.pp",
              "milestone-task-with-duration-0.mpp",
              "milestone-task-with-duration-0.xer",
              "milestone-task-with-duration-0-ms.xml",
              "milestone-task-with-duration-0-p6.xml"])
  @ParameterizedTest
  fun `verify a task with duration 0 is imported successfully as a milestone`(file: Resource) {
    projectImportService.import(project, readProject(file), false, null, null, null, null)

    val milestones = milestoneRepository.findAllByProjectIdentifier(projectIdentifier)

    milestones.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].name).isEqualTo("task with duration 0")
      assertThat(this[0].date).isEqualTo(LocalDate.of(2022, 4, 29))
      assertThat(this[0].header).isTrue
      assertThat(this[0].type).isEqualTo(MilestoneTypeEnum.PROJECT)
    }

    projectEventStoreUtils
        .verifyContains(MilestoneEventAvro::class.java, MilestoneEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "task with duration 0" }

    projectEventStoreUtils.verifyContains(
        MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyNumberOfEvents(5)
  }
}
