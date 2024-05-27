/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAction
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskConstraintCustomization
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.exceptions.BatchIdentifierTypeNotSupportedException
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.NamedEnumReference
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.from
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_CONSTRAINT_VALIDATION_ERROR_REASON_DEACTIVATED
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.request.UpdateTaskConstraintSelectionResource
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.TaskConstraintSelectionListResource
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.TaskConstraintSelectionResource
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.COMMON_UNDERSTANDING
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.EQUIPMENT
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.INFORMATION
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.MATERIAL
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintSelection
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException

@EnableAllKafkaListeners
class TaskConstraintSelectionIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: TaskConstraintSelectionController

  private val selectedConstraints = arrayOf(INFORMATION, EQUIPMENT)
  private val saveSelectionResource: UpdateTaskConstraintSelectionResource =
      UpdateTaskConstraintSelectionResource(selectedConstraints.toSet())

  private val task by lazy { repositories.findTaskWithDetails(getIdentifier("task").asTaskId())!! }
  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()
    setAuthentication("userCsm2")
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify updating fails if project does not exist`() {
    createSelection("task")

    assertThatThrownBy {
          cut.updateConstraintSelection(
              ProjectId(), task.identifier, saveSelectionResource, ETag.from(0L))
        }
        .isInstanceOf(AccessDeniedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify updating fails if task does not exist`() {
    assertThatThrownBy {
          cut.updateConstraintSelection(
              projectIdentifier, TaskId(), saveSelectionResource, ETag.from(0L))
        }
        .isInstanceOf(AccessDeniedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find constraint selection fails if project does not exist`() {
    assertThatThrownBy { cut.findConstraintSelection(ProjectId(), task.identifier) }
        .isInstanceOf(AccessDeniedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find constraint selection fails if task does not exist`() {
    assertThatThrownBy { cut.findConstraintSelection(projectIdentifier, TaskId()) }
        .isInstanceOf(AccessDeniedException::class.java)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find constraint selection of a task if an empty task selection exists`() {
    createSelection("task")

    val responseEntity = cut.findConstraintSelection(projectIdentifier, task.identifier)

    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(responseEntity.body).isNotNull
    assertThat(responseEntity.body!!.items).isEmpty()

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find constraint selection of a task`() {
    createSelection("task", COMMON_UNDERSTANDING, MATERIAL)

    val responseEntity = cut.findConstraintSelection(projectIdentifier, task.identifier)

    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(responseEntity.body).isNotNull
    assertThat(responseEntity.body!!.items).hasSize(2)
    assertThat(responseEntity.body!!.items)
        .extracting<TaskConstraintEnum>(NamedEnumReference<TaskConstraintEnum>::key)
        .containsOnly(COMMON_UNDERSTANDING, MATERIAL)

    projectEventStoreUtils.verifyEmpty()
  }

  @Nested
  @DisplayName("Find constraint selections for multiple tasks")
  inner class TaskConstraintSelectionsForMultipleTasks {

    @Test
    fun succeeds() {
      createSelection("task", COMMON_UNDERSTANDING, MATERIAL)
      eventStreamGenerator.submitTask(asReference = "task2")

      createSelection("task2", EQUIPMENT)

      val searchResource =
          BatchRequestResource(setOf(task.identifier.toUuid(), getIdentifier("task2")))
      val responseEntity: ResponseEntity<TaskConstraintSelectionListResource> =
          cut.findConstraintSelections(
              projectIdentifier, searchResource, BatchRequestIdentifierType.TASK)

      assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(responseEntity.body).isNotNull
      assertThat(responseEntity.body!!.selections).hasSize(2)
      assertThat(responseEntity.body!!.selections)
          .extracting<UUID>(TaskConstraintSelectionResource::taskIdentifier)
          .containsOnly(task.identifier.toUuid(), getIdentifier("task2"))
      assertThat(responseEntity.body!!.selections)
          .flatExtracting<NamedEnumReference<TaskConstraintEnum>>(
              TaskConstraintSelectionResource::items)
          .extracting<TaskConstraintEnum>(NamedEnumReference<TaskConstraintEnum>::key)
          .containsOnly(COMMON_UNDERSTANDING, MATERIAL, EQUIPMENT)

      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `is empty`() {
      createSelection("task")
      eventStreamGenerator.submitTask(asReference = "task2")

      createSelection("task2")

      val searchResource =
          BatchRequestResource(setOf(task.identifier.toUuid(), getIdentifier("task2")))
      val responseEntity: ResponseEntity<TaskConstraintSelectionListResource> =
          cut.findConstraintSelections(
              projectIdentifier, searchResource, BatchRequestIdentifierType.TASK)

      assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(responseEntity.body).isNotNull
      assertThat(responseEntity.body!!.selections).hasSize(2)
      assertThat(responseEntity.body!!.selections)
          .extracting<UUID>(TaskConstraintSelectionResource::taskIdentifier)
          .containsOnly(task.identifier.toUuid(), getIdentifier("task2"))
      assertThat(responseEntity.body!!.selections)
          .flatExtracting<NamedEnumReference<TaskConstraintEnum>>(
              TaskConstraintSelectionResource::items)
          .extracting<TaskConstraintEnum>(NamedEnumReference<TaskConstraintEnum>::key)
          .isEmpty()

      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `fails for non supported identifier type`() {
      assertThatThrownBy {
            cut.findConstraintSelections(
                projectIdentifier,
                BatchRequestResource(setOf(task.identifier.toUuid())),
                BatchRequestIdentifierType.DAYCARD)
          }
          .usingRecursiveComparison()
          .withStrictTypeChecking()
          .isEqualTo(
              BatchIdentifierTypeNotSupportedException(
                  COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED))

      projectEventStoreUtils.verifyEmpty()
    }
  }

  @Test
  fun `If a constraint selection has empty set of constraints - update succeeds`() {
    assertThat(
            cut.updateConstraintSelection(
                    projectIdentifier,
                    task.identifier,
                    UpdateTaskConstraintSelectionResource(emptySet()),
                    ETag.from(0L))
                .statusCode)
        .isEqualTo(HttpStatus.OK)

    // If nothing has changed, no event is expected
    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `If no selection exists and a selection is passed - create succeeds`() {
    val responseEntity =
        cut.updateConstraintSelection(
            projectIdentifier,
            task.identifier,
            UpdateTaskConstraintSelectionResource(setOf(MATERIAL)),
            ETag.from(0L))

    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(responseEntity.body).isNotNull

    val resource: TaskConstraintSelectionResource = responseEntity.body!!
    assertThat(resource.taskIdentifier.asTaskId()).isEqualTo(task.identifier)
    assertThat(resource.items).hasSize(1)
    assertThat(resource.items.first().key).isEqualTo(MATERIAL)

    val createdEvent =
        projectEventStoreUtils.verifyContainsAndGet(
            TaskActionSelectionEventAvro::class.java, CREATED, false)
    assertThat(createdEvent.aggregate.task.identifier).isEqualTo(task.identifier.toString())
    assertThat(createdEvent.aggregate.actions).isEmpty()

    val updatedEvent =
        projectEventStoreUtils.verifyContainsAndGet(
            TaskActionSelectionEventAvro::class.java, UPDATED, false)
    assertThat(updatedEvent.aggregate.task.identifier).isEqualTo(task.identifier.toString())
    assertThat(updatedEvent.aggregate.actions).hasSize(1)
    assertThat(updatedEvent.aggregate.actions.first()).isEqualTo(TaskActionEnumAvro.MATERIAL)

    projectEventStoreUtils.verifyContainsInSequence(
        listOf(TaskActionSelectionEventAvro::class.java, TaskActionSelectionEventAvro::class.java))
  }

  @Test
  fun `If selection exists and empty updated selection is passed - delete succeeds`() {
    createSelection("task")
    val responseEntity =
        cut.updateConstraintSelection(
            projectIdentifier,
            task.identifier,
            UpdateTaskConstraintSelectionResource(emptySet()),
            ETag.from(0L))

    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(responseEntity.body).isNotNull

    val resource: TaskConstraintSelectionResource = responseEntity.body!!
    assertThat(resource.taskIdentifier.asTaskId()).isEqualTo(task.identifier)
    assertThat(resource.items).isEmpty()

    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            TaskActionSelectionEventAvro::class.java, DELETED)
    assertThat(event.aggregate.task.identifier).isEqualTo(task.identifier.toString())
    assertThat(event.aggregate.actions).isEmpty()
  }

  @Test
  fun `Constraint selection exists and updated selection is passed - update succeeds`() {
    createSelection("task", *selectedConstraints)
    val taskIdentifier = task.identifier
    val resource = UpdateTaskConstraintSelectionResource(arrayOf(INFORMATION).toSet())

    // we update the selection
    val responseEntityUpdate =
        cut.updateConstraintSelection(projectIdentifier, taskIdentifier, resource, ETag.from(0L))
    assertThat(responseEntityUpdate.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(responseEntityUpdate.body).isNotNull

    val taskConstraintSelectionResource = responseEntityUpdate.body!!
    assertThat(taskConstraintSelectionResource.taskIdentifier.asTaskId()).isEqualTo(taskIdentifier)
    assertThat(taskConstraintSelectionResource.items)
        .hasSize(1)
        .extracting<TaskConstraintEnum>(NamedEnumReference<TaskConstraintEnum>::key)
        .containsExactly(INFORMATION)

    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            TaskActionSelectionEventAvro::class.java, UPDATED)
    assertThat(event.aggregate.task.identifier).isEqualTo(task.identifier.toString())
    assertThat(event.aggregate.actions).containsOnly(TaskActionEnumAvro.INFORMATION)
  }

  @Test
  fun `If no selection exists and a selection with inactive constraints is passed - update fails`() {
    eventStreamGenerator.submitTaskConstraintCustomization {
      it.active = false
      it.key = TaskActionEnumAvro.MATERIAL
    }

    projectEventStoreUtils.reset()

    assertThatThrownBy {
          cut.updateConstraintSelection(
              projectIdentifier,
              task.identifier,
              UpdateTaskConstraintSelectionResource(arrayOf(MATERIAL).toSet()),
              ETag.from(0L))
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(
            PreconditionViolationException(TASK_CONSTRAINT_VALIDATION_ERROR_REASON_DEACTIVATED))

    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            TaskActionSelectionEventAvro::class.java, CREATED)
    assertThat(event.aggregate.task.identifier).isEqualTo(task.identifier.toString())
  }

  @Test
  fun `If selection exists and an updated selection with inactive constraints is passed - update fails`() {
    eventStreamGenerator.submitTaskConstraintCustomization {
      it.active = false
      it.key = TaskActionEnumAvro.MATERIAL
    }

    createSelection("task", constraints = arrayOf(MATERIAL, EQUIPMENT))
    projectEventStoreUtils.reset()

    assertThatThrownBy {
          cut.updateConstraintSelection(
              projectIdentifier,
              task.identifier,
              UpdateTaskConstraintSelectionResource(arrayOf(MATERIAL).toSet()),
              ETag.from(0L))
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(
            PreconditionViolationException(TASK_CONSTRAINT_VALIDATION_ERROR_REASON_DEACTIVATED))

    projectEventStoreUtils.verifyEmpty()
  }

  private fun createSelection(
      taskReference: String,
      vararg constraints: TaskConstraintEnum
  ): TaskConstraintSelection {
    eventStreamGenerator.submitTaskAction(asReference = "$taskReference-selection") {
      it.task = getByReference(taskReference)
      it.actions = constraints.map { constraint -> TaskActionEnumAvro.valueOf(constraint.name) }
    }

    return repositories.findTaskConstraintSelectionByIdentifier(
        getIdentifier("$taskReference-selection"))!!
  }
}
