/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest.taskassignment

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.UpdateBatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.toEtag
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_UNASSIGN_POSSIBLE_WHEN_STATUS_IS_NOT_CLOSED_OR_ACCEPTED
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.TaskController
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.SaveTaskResourceBuilder
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.SaveTaskResourceWithIdentifierAndVersion
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.OK

@EnableAllKafkaListeners
class TaskUnassignmentIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var taskAssignmentController: TaskAssignmentController
  @Autowired private lateinit var taskController: TaskController

  private val unassignedDraftTask: Task by lazy {
    repositories.findTaskWithDetails(getIdentifier("unassignedDraftTask").asTaskId())!!
  }

  private val assignedDraftTask: Task by lazy {
    repositories.findTaskWithDetails(getIdentifier("assignedDraftTask").asTaskId())!!
  }

  private val openTask: Task by lazy {
    repositories.findTaskWithDetails(getIdentifier("taskOpen").asTaskId())!!
  }

  private val startedTask: Task by lazy {
    repositories.findTaskWithDetails(getIdentifier("taskStarted").asTaskId())!!
  }

  private val closedTask: Task by lazy {
    repositories.findTaskWithDetails(getIdentifier("taskClosed").asTaskId())!!
  }

  private val acceptedTask: Task by lazy {
    repositories.findTaskWithDetails(getIdentifier("taskAccepted").asTaskId())!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitUser("userCsm")
        .submitParticipantG3("participantCsm") { it.role = ParticipantRoleEnumAvro.CSM }
        .submitTask("unassignedDraftTask") {
          it.assignee = null
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask("assignedDraftTask") {
          it.assignee = getByReference("participant")
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask("taskOpen") {
          it.assignee = getByReference("participant")
          it.status = TaskStatusEnumAvro.OPEN
        }
        .submitTask("taskStarted") {
          it.assignee = getByReference("participant")
          it.status = TaskStatusEnumAvro.STARTED
        }
        .submitTask("taskClosed") {
          it.assignee = getByReference("participant")
          it.status = TaskStatusEnumAvro.CLOSED
        }
        .submitTask("taskAccepted") {
          it.assignee = getByReference("participant")
          it.status = TaskStatusEnumAvro.ACCEPTED
        }

    setAuthentication(getIdentifier("userCsm"))

    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify unassign draft task`() {
    val response = taskAssignmentController.unassignTask(assignedDraftTask.identifier)

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.UNASSIGNED, 1, true)
  }

  @Test
  fun `verify unassign open task`() {
    val response = taskAssignmentController.unassignTask(openTask.identifier)

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.UNASSIGNED, 1, true)
  }

  @Test
  fun `verify unassign started task`() {
    val response = taskAssignmentController.unassignTask(startedTask.identifier)

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.UNASSIGNED, 1, true)
  }

  @Test
  fun `verify unassign task for closed task not possible`() {
    assertThatThrownBy { taskAssignmentController.unassignTask(closedTask.identifier) }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(
            PreconditionViolationException(
                TASK_VALIDATION_ERROR_UNASSIGN_POSSIBLE_WHEN_STATUS_IS_NOT_CLOSED_OR_ACCEPTED))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify unassign task for accepted task not possible`() {
    assertThatThrownBy { taskAssignmentController.unassignTask(acceptedTask.identifier) }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(
            PreconditionViolationException(
                TASK_VALIDATION_ERROR_UNASSIGN_POSSIBLE_WHEN_STATUS_IS_NOT_CLOSED_OR_ACCEPTED))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify unassign tasks`() {
    val unassignTasksResource =
        BatchRequestResource(
            setOf(
                unassignedDraftTask.identifier.toUuid(),
                openTask.identifier.toUuid(),
                startedTask.identifier.toUuid()))

    val response = taskAssignmentController.unassignTasks(unassignTasksResource)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull

    // expect three tasks to be unassigned
    assertThat(response.body!!.tasks).hasSize(3)

    // expect only 2 events since one of the tasks was already unassigned
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.UNASSIGNED, 2, false)
  }

  @Test
  fun `verify unassign tasks for closed tasks not possible`() {
    assertThatThrownBy {
          taskAssignmentController.unassignTasks(
              BatchRequestResource(setOf(closedTask.identifier.toUuid())))
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(
            PreconditionViolationException(
                TASK_VALIDATION_ERROR_UNASSIGN_POSSIBLE_WHEN_STATUS_IS_NOT_CLOSED_OR_ACCEPTED))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify unassign tasks for accepted tasks not possible`() {
    assertThatThrownBy {
          taskAssignmentController.unassignTasks(
              BatchRequestResource(setOf(acceptedTask.identifier.toUuid())))
        }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(
            PreconditionViolationException(
                TASK_VALIDATION_ERROR_UNASSIGN_POSSIBLE_WHEN_STATUS_IS_NOT_CLOSED_OR_ACCEPTED))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update and unassign task`() {
    val task = repositories.findTaskWithDetails(getIdentifier("task").asTaskId())!!
    val saveTaskResource =
        SaveTaskResourceBuilder()
            .setProjectId(getIdentifier("project").asProjectId())
            .setProjectCraftId(getIdentifier("projectCraft").asProjectCraftId())
            .setName("updated name")
            .setDescription("description")
            .setLocation("location")
            .setStatus(OPEN)
            .setAssigneeId(null)
            .setWorkAreaId(null)
            .createSaveTaskResource()

    taskController.updateTask(openTask.identifier, saveTaskResource, task.toEtag()).apply {
      assertThat(statusCode).isEqualTo(OK)
      body!!.apply {
        assertThat(name).isEqualTo(saveTaskResource.name)
        assertThat(description).isEqualTo(saveTaskResource.description)
        assertThat(location).isEqualTo(saveTaskResource.location)
        assertThat(project.identifier.asProjectId()).isEqualTo(saveTaskResource.projectId)
        assertThat(workArea).isNull()
        assertThat(editDate).isNotNull
        assertThat(assignee).isNull()
      }
    }

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.UPDATED, 1, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.UNASSIGNED, 1, false)
  }

  @Test
  fun `verify update and unassign tasks`() {
    val updateUnassigningAnAssignedTask =
        SaveTaskResourceWithIdentifierAndVersion(
            id = openTask.identifier.toUuid(),
            version = openTask.version,
            name = "updated name",
            description = openTask.description,
            location = openTask.location,
            status = openTask.status,
            projectId = openTask.project.identifier,
            projectCraftId = openTask.projectCraft.identifier,
            assigneeId = null, // unassign!
            workAreaId = openTask.workArea?.identifier)
    val unrelatedUpdateOfUnassignedTask =
        SaveTaskResourceWithIdentifierAndVersion(
            id = unassignedDraftTask.identifier.toUuid(),
            version = unassignedDraftTask.version,
            name = unassignedDraftTask.name,
            description = unassignedDraftTask.description,
            location = "unrelated change",
            status = unassignedDraftTask.status,
            projectId = unassignedDraftTask.project.identifier,
            projectCraftId = unassignedDraftTask.projectCraft.identifier,
            assigneeId = unassignedDraftTask.assignee?.identifier,
            workAreaId = unassignedDraftTask.workArea?.identifier)
    val unrelatedUpdateOfAssignedTask =
        SaveTaskResourceWithIdentifierAndVersion(
            id = assignedDraftTask.identifier.toUuid(),
            version = assignedDraftTask.version,
            name = assignedDraftTask.name,
            description = assignedDraftTask.description,
            location = "unrelated change",
            status = assignedDraftTask.status,
            projectId = assignedDraftTask.project.identifier,
            projectCraftId = assignedDraftTask.projectCraft.identifier,
            assigneeId = assignedDraftTask.assignee?.identifier,
            workAreaId = assignedDraftTask.workArea?.identifier)
    val taskUpdates: Collection<SaveTaskResourceWithIdentifierAndVersion> =
        listOf(
            updateUnassigningAnAssignedTask,
            unrelatedUpdateOfUnassignedTask,
            unrelatedUpdateOfAssignedTask)

    val response = taskController.updateTasks(UpdateBatchRequestResource(taskUpdates))

    assertThat(response.statusCode).isEqualTo(OK)
    val updatedTasksByIdentifier = response.body!!.tasks.associateBy { it.id }
    assertThatFieldsMatchAfterUpdate(taskUpdates, updatedTasksByIdentifier)
    assertThat(updatedTasksByIdentifier[updateUnassigningAnAssignedTask.id]?.assignee).isNull()

    // 3 update events expected but only 1 was unassigned in the update, so only 1 unassigned event
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.UPDATED, 3, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.UNASSIGNED, 1, false)
  }

  private fun assertThatFieldsMatchAfterUpdate(
      taskUpdates: Collection<SaveTaskResourceWithIdentifierAndVersion>,
      updatedTasksByIdentifier: Map<UUID, TaskResource>
  ) {
    assertThat(taskUpdates.size).isEqualTo(updatedTasksByIdentifier.values.size)

    taskUpdates.forEach {
      updatedTasksByIdentifier[it.id]!!.apply {
        assertThat(name).isEqualTo(it.name)
        assertThat(description).isEqualTo(it.description)
        assertThat(location).isEqualTo(it.location)
        assertThat(project.identifier.asProjectId()).isEqualTo(it.projectId)
        assertThat(workArea).isNull()
        assertThat(editDate).isNotNull
      }
    }
  }
}
