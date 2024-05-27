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
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.test.FileSource
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectController
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.MILESTONE
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.TASK
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum.FINISH_TO_START
import com.bosch.pt.iot.smartsite.project.relation.repository.RelationRepository
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.data.domain.Pageable.unpaged

@EnableAllKafkaListeners
class ImportRelationIntegrationTest : AbstractImportIntegrationTest() {

  @Autowired private lateinit var projectImportService: ProjectImportService

  @Autowired private lateinit var projectRepository: ProjectRepository

  @Autowired private lateinit var taskRepository: TaskRepository

  @Autowired private lateinit var milestoneRepository: MilestoneRepository

  @Autowired private lateinit var relationRepository: RelationRepository

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
              "relation-task-task.pp",
              "relation-task-task.mpp",
              "relation-task-task.xer",
              "relation-task-task-ms.xml",
              "relation-task-task-p6.xml"])
  @ParameterizedTest
  fun `verify a task-task relation is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), false, null, null, null, null)

    val tasks = taskRepository.findAllByProjectIdentifier(projectIdentifier, unpaged()).content

    val relations = relationRepository.findAllByProjectIdentifier(projectIdentifier)

    relations.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].type).isEqualTo(FINISH_TO_START)
      assertThat(this[0].source.identifier).isEqualTo(tasks[0].identifier.toUuid())
      assertThat(this[0].source.type).isEqualTo(TASK)
      assertThat(this[0].target.identifier).isEqualTo(tasks[1].identifier.toUuid())
      assertThat(this[0].target.type).isEqualTo(TASK)
    }

    projectEventStoreUtils.verifyContains(
        RelationEventAvro::class.java, RelationEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 2, false)
        .map { it.aggregate.name }
        .all { listOf("task1", "task2").contains(it) }

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

    projectEventStoreUtils.verifyNumberOfEvents(11)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "relation-task-milestone.pp",
              "relation-task-milestone.mpp",
              "relation-task-milestone.xer",
              "relation-task-milestone-ms.xml",
              "relation-task-milestone-p6.xml"])
  @ParameterizedTest
  fun `verify a task-milestone relation is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), false, null, null, null, null)

    val tasks = taskRepository.findAllByProjectIdentifier(projectIdentifier, unpaged()).content
    val milestones = milestoneRepository.findAllByProjectIdentifier(projectIdentifier)
    val relations = relationRepository.findAllByProjectIdentifier(projectIdentifier)

    relations.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].type).isEqualTo(FINISH_TO_START)
      assertThat(this[0].source.identifier).isEqualTo(tasks[0].identifier.toUuid())
      assertThat(this[0].source.type).isEqualTo(TASK)
      assertThat(this[0].target.identifier).isEqualTo(milestones[0].identifier.toUuid())
      assertThat(this[0].target.type).isEqualTo(MILESTONE)
    }

    projectEventStoreUtils.verifyContains(
        RelationEventAvro::class.java, RelationEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "task1" }

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "milestone1" }

    projectEventStoreUtils.verifyContains(
        MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 2, false)

    projectEventStoreUtils.verifyNumberOfEvents(11)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "relation-milestone-task.pp",
              "relation-milestone-task.mpp",
              "relation-milestone-task.xer",
              "relation-milestone-task-ms.xml",
              "relation-milestone-task-p6.xml"])
  @ParameterizedTest
  fun `verify a milestone-task relation is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), false, null, null, null, null)

    val tasks = taskRepository.findAllByProjectIdentifier(projectIdentifier, unpaged()).content
    val milestones = milestoneRepository.findAllByProjectIdentifier(projectIdentifier)
    val relations = relationRepository.findAllByProjectIdentifier(projectIdentifier)

    relations.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].type).isEqualTo(FINISH_TO_START)
      assertThat(this[0].source.identifier).isEqualTo(milestones[0].identifier.toUuid())
      assertThat(this[0].source.type).isEqualTo(MILESTONE)
      assertThat(this[0].target.identifier).isEqualTo(tasks[0].identifier.toUuid())
      assertThat(this[0].target.type).isEqualTo(TASK)
    }

    projectEventStoreUtils.verifyContains(
        RelationEventAvro::class.java, RelationEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "task1" }

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "milestone1" }

    projectEventStoreUtils.verifyContains(
        MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 2, false)

    projectEventStoreUtils.verifyNumberOfEvents(11)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "relation-milestone-milestone.pp",
              "relation-milestone-milestone.mpp",
              "relation-milestone-milestone.xer",
              "relation-milestone-milestone-ms.xml",
              "relation-milestone-milestone-p6.xml"])
  @ParameterizedTest
  fun `verify a milestone-milestone relation is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), false, null, null, null, null)

    val milestones = milestoneRepository.findAllByProjectIdentifier(projectIdentifier)
    val relations = relationRepository.findAllByProjectIdentifier(projectIdentifier)

    relations.apply {
      assertThat(this).hasSize(1)
      assertThat(this[0].type).isEqualTo(FINISH_TO_START)
      assertThat(this[0].source.identifier).isEqualTo(milestones[0].identifier.toUuid())
      assertThat(this[0].source.type).isEqualTo(MILESTONE)
      assertThat(this[0].target.identifier).isEqualTo(milestones[1].identifier.toUuid())
      assertThat(this[0].target.type).isEqualTo(MILESTONE)
    }

    projectEventStoreUtils.verifyContains(
        RelationEventAvro::class.java, RelationEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.CREATED, 2, false)
        .map { it.aggregate.name }
        .all { listOf("milestone1", "milestone2").contains(it) }

    projectEventStoreUtils.verifyContains(
        MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.CREATED, 2, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 2, false)

    projectEventStoreUtils.verifyNumberOfEvents(9)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "relation-task-with-2-successors.pp",
              "relation-task-with-2-successors.mpp",
              "relation-task-with-2-successors.xer",
              "relation-task-with-2-successors-ms.xml",
              "relation-task-with-2-successors-p6.xml"])
  @ParameterizedTest
  fun `verify a task-with-2-successor relation is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), false, null, null, null, null)

    val tasks = taskRepository.findAllByProjectIdentifier(projectIdentifier, unpaged()).content

    val relations = relationRepository.findAllByProjectIdentifier(projectIdentifier)

    relations.apply {
      assertThat(this).hasSize(2)

      assertThat(this[0].type).isEqualTo(FINISH_TO_START)
      assertThat(this[0].source.identifier).isEqualTo(tasks[0].identifier.toUuid())
      assertThat(this[0].source.type).isEqualTo(TASK)
      assertThat(this[0].target.identifier).isEqualTo(tasks[1].identifier.toUuid())
      assertThat(this[0].target.type).isEqualTo(TASK)

      assertThat(this[1].type).isEqualTo(FINISH_TO_START)
      assertThat(this[1].source.identifier).isEqualTo(tasks[0].identifier.toUuid())
      assertThat(this[1].source.type).isEqualTo(TASK)
      assertThat(this[1].target.identifier).isEqualTo(tasks[2].identifier.toUuid())
      assertThat(this[1].target.type).isEqualTo(TASK)
    }

    projectEventStoreUtils.verifyContains(
        RelationEventAvro::class.java, RelationEventEnumAvro.CREATED, 2, false)

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 3, false)
        .map { it.aggregate.name }
        .all { listOf("task01", "task02", "task03").contains(it) }

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 3, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 3, false)

    projectEventStoreUtils.verifyNumberOfEvents(15)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "relation-task-with-2-predecessors.pp",
              "relation-task-with-2-predecessors.mpp",
              "relation-task-with-2-predecessors.xer",
              "relation-task-with-2-predecessors-ms.xml",
              "relation-task-with-2-predecessors-p6.xml"])
  @ParameterizedTest
  fun `verify a task-with-2-predecessor relation is imported successfully`(file: Resource) {
    projectImportService.import(project, readProject(file), false, null, null, null, null)

    val tasks = taskRepository.findAllByProjectIdentifier(projectIdentifier, unpaged()).content
    val milestones = milestoneRepository.findAllByProjectIdentifier(projectIdentifier)
    val relations = relationRepository.findAllByProjectIdentifier(projectIdentifier)

    relations.apply {
      assertThat(this).hasSize(2)

      assertThat(this[0].type).isEqualTo(FINISH_TO_START)
      assertThat(this[0].source.identifier).isEqualTo(milestones[0].identifier.toUuid())
      assertThat(this[0].source.type).isEqualTo(MILESTONE)
      assertThat(this[0].target.identifier).isEqualTo(tasks[1].identifier.toUuid())
      assertThat(this[0].target.type).isEqualTo(TASK)

      assertThat(this[1].type).isEqualTo(FINISH_TO_START)
      assertThat(this[1].source.identifier).isEqualTo(tasks[0].identifier.toUuid())
      assertThat(this[1].source.type).isEqualTo(TASK)
      assertThat(this[1].target.identifier).isEqualTo(tasks[1].identifier.toUuid())
      assertThat(this[1].target.type).isEqualTo(TASK)
    }

    projectEventStoreUtils.verifyContains(
        RelationEventAvro::class.java, RelationEventEnumAvro.CREATED, 2, false)

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 2, false)
        .map { it.aggregate.name }
        .all { listOf("task01", "task02").contains(it) }

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 2, false)

    projectEventStoreUtils
        .verifyContainsAndGet(
            MilestoneEventAvro::class.java, MilestoneEventEnumAvro.CREATED, 1, false)
        .map { it.aggregate.name }
        .all { it == "milestone01" }

    projectEventStoreUtils.verifyContains(
        MilestoneListEventAvro::class.java, MilestoneListEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 3, false)

    projectEventStoreUtils.verifyNumberOfEvents(15)
  }

  @FileSource(
      container = "project-import-testdata",
      files =
          [
              "relation-3-non-finish-to-start-relations.pp",
              "relation-3-non-finish-to-start-relations.mpp",
              "relation-3-non-finish-to-start-relations.xer",
              "relation-3-non-finish-to-start-relations-ms.xml",
              "relation-3-non-finish-to-start-relations-p6.xml"])
  @ParameterizedTest
  fun `verify only finish-to-start relations are imported`(file: Resource) {
    projectImportService.import(project, readProject(file), false, null, null, null, null)

    val relations = relationRepository.findAllByProjectIdentifier(projectIdentifier)

    assertThat(relations).hasSize(0)

    projectEventStoreUtils.verifyContains(
        RelationEventAvro::class.java, RelationEventEnumAvro.CREATED, 0, false)

    projectEventStoreUtils.verifyContains(
        TaskScheduleEventAvro::class.java, TaskScheduleEventEnumAvro.CREATED, 5, false)

    projectEventStoreUtils
        .verifyContainsAndGet(TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 5, false)
        .map { it.aggregate.name }
        .all { listOf("task01", "task02", "task03", "task04", "task05").contains(it) }

    projectEventStoreUtils.verifyContains(
        ProjectCraftEventG2Avro::class.java, ProjectCraftEventEnumAvro.CREATED, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectCraftListEventAvro::class.java, ProjectCraftListEventEnumAvro.ITEMADDED, 1, false)

    projectEventStoreUtils.verifyContains(ProjectImportStartedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ProjectImportFinishedEventAvro::class.java, null, 1, false)

    projectEventStoreUtils.verifyContains(
        ExternalIdEventAvro::class.java, ExternalIdEventEnumAvro.CREATED, 5, false)

    projectEventStoreUtils.verifyNumberOfEvents(19)
  }
}
