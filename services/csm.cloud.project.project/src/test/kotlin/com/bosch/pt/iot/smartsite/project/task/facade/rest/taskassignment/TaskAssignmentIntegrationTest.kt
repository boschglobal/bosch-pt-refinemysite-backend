/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest.taskassignment

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.CreateBatchRequestResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.UpdateBatchRequestResource
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_BAD_REQUEST
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.transaction.messages.BatchOperationStartedEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.toEtag
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_CLOSED_OR_ACCEPTED_TASK_ASSIGNMENT_FORBIDDEN
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_NOT_OF_SAME_PROJECT
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.TaskController
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.SaveTaskResourceBuilder
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.AssignTaskListToParticipantResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.AssignTaskToParticipantResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.CreateTaskBatchResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.SaveTaskResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.SaveTaskResourceWithIdentifierAndVersion
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import java.util.UUID
import java.util.UUID.randomUUID
import jakarta.validation.ConstraintViolationException
import org.apache.commons.lang3.StringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.OK
import org.springframework.test.util.ReflectionTestUtils

@EnableAllKafkaListeners
class TaskAssignmentIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var taskController: TaskController
  @Autowired private lateinit var taskAssignmentController: TaskAssignmentController

  private val csmParticipant: Participant by lazy {
    repositories.findParticipant(getIdentifier("participantCsm"))!!
  }

  private val participant: Participant by lazy {
    repositories.findParticipant(getIdentifier("participant"))!!
  }

  private val unassignedDraftTask: Task by lazy {
    repositories.findTaskWithDetails(getIdentifier("unassignedDraftTask").asTaskId())!!
  }

  private val assignedDraftTask: Task by lazy {
    repositories.findTaskWithDetails(getIdentifier("assignedDraftTask").asTaskId())!!
  }

  private val openTask: Task by lazy {
    repositories.findTaskWithDetails(getIdentifier("taskOpen").asTaskId())!!
  }

  private val closedTask: Task by lazy {
    repositories.findTaskWithDetails(getIdentifier("taskClosed").asTaskId())!!
  }

  private val acceptedTask: Task by lazy {
    repositories.findTaskWithDetails(getIdentifier("taskAccepted").asTaskId())!!
  }

  private val otherProjectTask: Task by lazy {
    repositories.findTaskWithDetails(getIdentifier("otherProjectTask").asTaskId())!!
  }

  private val task: Task by lazy {
    repositories.findTaskWithDetails(getIdentifier("task").asTaskId())!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitUser("userCsm")
        .submitParticipantG3("participantCsm") { it.role = ParticipantRoleEnumAvro.CSM }
        .setUserContext("userCsm")
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
        .submitProject("otherProject")
        .submitParticipantG3("otherProjectCsm") {
          it.role = ParticipantRoleEnumAvro.CSM
          it.user = getByReference("userCsm")
        }
        .submitTask("otherProjectTask") {
          it.assignee = null
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .setLastIdentifierForType(PROJECT.value, getByReference("project"))

    setAuthentication(getIdentifier("userCsm"))

    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify create and assign task without assign`() {
    val saveTaskResource = buildSaveTaskResource(assign = false, send = false)
    val response = taskController.createTask(null, saveTaskResource)

    assertThat(response.statusCode).isEqualTo(CREATED)
    assertThat(response.body).isNotNull
    assertThatFieldsMatchAfterCreate(saveTaskResource, response.body!!)
    assertThatIsUnassigned(response.body!!)

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 1, true)
  }

  @Test
  fun `verify create and assign task with assign`() {
    val saveTaskResource = buildSaveTaskResource(assign = true, send = false)
    val response = taskController.createTask(null, saveTaskResource)

    assertThat(response.statusCode).isEqualTo(CREATED)
    assertThat(response.body).isNotNull
    assertThatFieldsMatchAfterCreate(saveTaskResource, response.body!!)
    assertThatIsAssignedAndNotSent(setOf(response.body!!))

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 1, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.ASSIGNED, 1, false)
  }

  @Test
  fun `verify create and assign task with assign and send`() {
    val saveTaskResource = buildSaveTaskResource(assign = true, send = true)
    val response = taskController.createTask(null, saveTaskResource)

    assertThat(response.statusCode).isEqualTo(CREATED)
    assertThat(response.body).isNotNull
    assertThatFieldsMatchAfterCreate(saveTaskResource, response.body!!)
    assertThatIsAssignedAndSent(setOf(response.body!!))

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 1, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.ASSIGNED, 1, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.SENT, 1, false)
  }

  @Test
  fun `verify create and assign multiple tasks without assign`() {
    val firstCreateTask = buildCreateTaskBatchResource(assign = false, send = false)
    val secondCreateTask = buildCreateTaskBatchResource(assign = false, send = false)
    val input: Collection<CreateTaskBatchResource> = listOf(firstCreateTask, secondCreateTask)

    val response = taskController.createTasks(CreateBatchRequestResource(input))
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull

    val output = response.body!!.tasks
    assertThatFieldsMatchAfterCreate(input, output)
    assertThatIsUnassigned(output)

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 2, false)
  }

  @Test
  fun `verify create and assign multiple tasks with assign`() {
    val firstCreateTask = buildCreateTaskBatchResource(assign = true, send = false)
    val secondCreateTask = buildCreateTaskBatchResource(assign = true, send = false)
    val input: Collection<CreateTaskBatchResource> = listOf(firstCreateTask, secondCreateTask)

    val response = taskController.createTasks(CreateBatchRequestResource(input))
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull

    val output = response.body!!.tasks
    assertThatFieldsMatchAfterCreate(input, output)
    assertThatIsAssignedAndNotSent(output)

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 2, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.ASSIGNED, 2, false)
  }

  @Test
  fun `verify create and assign multiple tasks with assign and send`() {
    val firstCreateTask = buildCreateTaskBatchResource(assign = true, send = true)
    val secondCreateTask = buildCreateTaskBatchResource(assign = true, send = true)
    val input: Collection<CreateTaskBatchResource> = listOf(firstCreateTask, secondCreateTask)

    val response = taskController.createTasks(CreateBatchRequestResource(input))
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull

    val output = response.body!!.tasks
    assertThatFieldsMatchAfterCreate(input, output)
    assertThatIsAssignedAndSent(output)

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 2, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.ASSIGNED, 2, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.SENT, 2, false)
  }

  @Test
  fun `verify create and assign 100 tasks with assign and send`() {
    val input = ArrayList<CreateTaskBatchResource>()
    for (i in 0..99) {
      input.add(buildCreateTaskBatchResource(assign = true, send = true))
    }

    val response = taskController.createTasks(CreateBatchRequestResource(input))
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull

    val output = response.body!!.tasks
    assertThatFieldsMatchAfterCreate(input, output)
    assertThatIsAssignedAndSent(output)

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.CREATED, 100, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.ASSIGNED, 100, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.SENT, 100, false)
  }

  @Test
  fun `verify create and assign 101 tasks with assign and send fails`() {
    val input = ArrayList<CreateTaskBatchResource>()
    for (i in 0..100) {
      input.add(buildCreateTaskBatchResource(assign = true, send = true))
    }

    assertThatExceptionOfType(ConstraintViolationException::class.java)
        .isThrownBy { taskController.createTasks(CreateBatchRequestResource(input)) }
        .withMessage("createTasks.createBatchRequestResource.items: size must be between 0 and 100")

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update and assign task with assign`() {
    val saveTaskResource = buildSaveTaskResource(assign = true, send = false)
    val response =
        taskController.updateTask(unassignedDraftTask.identifier, saveTaskResource, task.toEtag())

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull
    assertThatFieldsMatchAfterUpdate(saveTaskResource, response.body!!)
    assertThatIsAssignedAndNotSent(setOf(response.body!!))

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.UPDATED, 1, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.ASSIGNED, 1, false)
  }

  @Test
  fun `verify update and assign task without assign`() {
    val saveTaskResource = buildSaveTaskResource(assign = false, send = false)
    val response =
        taskController.updateTask(unassignedDraftTask.identifier, saveTaskResource, task.toEtag())

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull
    assertThatFieldsMatchAfterUpdate(saveTaskResource, response.body!!)
    assertThatIsUnassigned(setOf(response.body!!))

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.UPDATED, 1, false)
  }

  @Test
  fun `verify update and assign task with assign and send`() {
    val saveTaskResource = buildSaveTaskResource(assign = true, send = true)
    val response =
        taskController.updateTask(unassignedDraftTask.identifier, saveTaskResource, task.toEtag())

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull
    assertThatFieldsMatchAfterUpdate(saveTaskResource, response.body!!)
    assertThatIsAssignedAndSent(setOf(response.body!!))

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.UPDATED, 1, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.ASSIGNED, 1, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.SENT, 1, false)
  }

  @Test
  fun `verify update and assign task with assign from open to draft status not possible`() {
    val saveTaskResource = buildSaveTaskResource(assign = true, send = false)
    val exception =
        assertThrows(PreconditionViolationException::class.java) {
          taskController.updateTask(openTask.identifier, saveTaskResource, task.toEtag())
        }

    assertThat(exception.messageKey).isEqualTo(SERVER_ERROR_BAD_REQUEST)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update and assign task does not modify edit date or version for unchanged task`() {
    val saveTaskResource = buildSaveTaskResource(assign = false, send = false)

    // initial update (might change task)
    val firstResponse =
        taskController.updateTask(unassignedDraftTask.identifier, saveTaskResource, task.toEtag())

    assertThat(firstResponse.statusCode).isEqualTo(OK)
    assertThat(firstResponse.body).isNotNull
    assertThat(firstResponse.body!!.version).isNotNull

    projectEventStoreUtils.reset()

    val versionAfterFirstUpdate = firstResponse.body!!.version
    val editDateAfterFirstUpdate = firstResponse.body!!.editDate

    // repeating the same update should not change the task
    val secondResponse =
        taskController.updateTask(
            unassignedDraftTask.identifier,
            saveTaskResource,
            ETag.from(StringUtils.wrap(versionAfterFirstUpdate.toString(), '"')))

    assertThat(secondResponse.statusCode).isEqualTo(OK)
    assertThat(secondResponse.body).isNotNull
    assertThat(secondResponse.body!!.version).isNotNull
    assertThat(secondResponse.body!!.version).isEqualTo(versionAfterFirstUpdate)
    assertThat(secondResponse.body!!.editDate).isEqualTo(editDateAfterFirstUpdate)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update and assign multiple tasks with assign`() {

    // Set a different field in the task to trigger an update
    unassignedDraftTask.location = "new updated location"
    unassignedDraftTask.assignee = participant
    val firstUpdateTask = buildUpdateTaskBatchResource(unassignedDraftTask)
    val input: Collection<SaveTaskResourceWithIdentifierAndVersion> = listOf(firstUpdateTask)

    val response = taskController.updateTasks(UpdateBatchRequestResource(input))
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull

    val output = response.body!!.tasks
    assertThatFieldsMatchAfterUpdate(input, output)
    assertThatIsAssignedAndNotSent(output)

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.UPDATED, 1, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.ASSIGNED, 1, false)
  }

  @Test
  fun `verify update and assign multiple tasks without assign`() {

    // Set a different field in the task to trigger an update
    unassignedDraftTask.location = "new updated location"
    val firstUpdateTask = buildUpdateTaskBatchResource(unassignedDraftTask)
    val input: Collection<SaveTaskResourceWithIdentifierAndVersion> = listOf(firstUpdateTask)

    val response = taskController.updateTasks(UpdateBatchRequestResource(input))
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull

    val output = response.body!!.tasks
    assertThatFieldsMatchAfterUpdate(input, output)
    assertThatIsUnassigned(output)

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.UPDATED, 1, false)
  }

  @Test
  fun `verify update and assign multiple tasks with assign and send`() {

    // Set a different field in the task to trigger an update
    unassignedDraftTask.location = "new updated location"
    unassignedDraftTask.assignee = participant
    unassignedDraftTask.status = OPEN
    val firstUpdateTask = buildUpdateTaskBatchResource(unassignedDraftTask)
    val input: Collection<SaveTaskResourceWithIdentifierAndVersion> = listOf(firstUpdateTask)

    val response = taskController.updateTasks(UpdateBatchRequestResource(input))
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull

    val output = response.body!!.tasks
    assertThatFieldsMatchAfterUpdate(input, output)
    assertThatIsAssignedAndSent(output)

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.UPDATED, 1, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.ASSIGNED, 1, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.SENT, 1, false)
  }

  @Test
  fun `verify update and assign 100 tasks with assign and send`() {

    // Create 100 tasks and prepare resource updates
    val input = ArrayList<SaveTaskResourceWithIdentifierAndVersion>()

    for (i in 0..99) {
      eventStreamGenerator.submitTask("task$i") { it.status = TaskStatusEnumAvro.DRAFT }

      val task = repositories.findTaskWithDetails(getIdentifier("task$i").asTaskId())!!
      task.location = "new updated location"
      task.assignee = participant
      task.status = OPEN
      input.add(buildUpdateTaskBatchResource(task))
    }

    // update 100 tasks
    val response = taskController.updateTasks(UpdateBatchRequestResource(input))
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull

    val output = response.body!!.tasks
    assertThat(output).hasSize(100)
    assertThatFieldsMatchAfterUpdate(input, output)
    assertThatIsAssignedAndSent(output)

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.UPDATED, 100, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.ASSIGNED, 100, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.SENT, 100, false)
  }

  @Test
  fun `verify update and assign 101 tasks with assign and send fails`() {

    // Create 101 tasks and prepare resource updates
    val input = ArrayList<SaveTaskResourceWithIdentifierAndVersion>()
    val newAssignee = getByReference("participant")

    for (i in 0..100) {
      eventStreamGenerator.submitTask("task$i") { it.assignee = newAssignee }

      val task = repositories.findTaskWithDetails(getIdentifier("task$i").asTaskId())!!
      task.location = "new updated location"
      input.add(buildUpdateTaskBatchResource(task))
    }

    assertThatExceptionOfType(ConstraintViolationException::class.java)
        .isThrownBy { taskController.updateTasks(UpdateBatchRequestResource(input)) }
        .withMessage("updateTasks.updateBatchResources.items: size must be between 0 and 100")

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update and assign multiple tasks with assign from open to draft status not possible`() {

    // Set the status of a open task to draft
    openTask.status = DRAFT
    val firstUpdateTask = buildUpdateTaskBatchResource(openTask)
    val exception =
        assertThrows(PreconditionViolationException::class.java) {
          taskController.updateTasks(UpdateBatchRequestResource(listOf(firstUpdateTask)))
        }

    assertThat(exception).isNotNull
    assertThat(exception.messageKey).isEqualTo(SERVER_ERROR_BAD_REQUEST)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update and assign multiple tasks for multiple projects not possible`() {
    openTask.name = "Update task name"
    val firstUpdateTask = buildUpdateTaskBatchResource(openTask)

    otherProjectTask.name = "Update task name"
    val secondUpdateTask = buildUpdateTaskBatchResource(otherProjectTask)

    val input: Collection<SaveTaskResourceWithIdentifierAndVersion> =
        listOf(firstUpdateTask, secondUpdateTask)
    val exception =
        assertThrows(PreconditionViolationException::class.java) {
          taskController.updateTasks(UpdateBatchRequestResource(input))
        }

    assertThat(exception).isNotNull
    assertThat(exception.messageKey).isEqualTo(TASK_VALIDATION_ERROR_NOT_OF_SAME_PROJECT)

    projectEventStoreUtils.verifyEmpty()
    projectEventStoreUtils.verifyContains(
        BatchOperationStartedEventAvro::class.java, TaskEventEnumAvro.UPDATED, 0, false)
  }

  @Test
  fun `verify update and assign multiple tasks does not modify edit date or version for unchanged task`() {

    // Set a different field in the task to trigger an update
    assignedDraftTask.location = "new updated location"
    val firstUpdateTask = buildUpdateTaskBatchResource(assignedDraftTask)

    val firstResponse =
        taskController.updateTasks(UpdateBatchRequestResource(listOf(firstUpdateTask)))
    assertThat(firstResponse.statusCode).isEqualTo(OK)
    assertThat(firstResponse.body).isNotNull

    projectEventStoreUtils.reset()

    val firstTaskResource = firstResponse.body!!.tasks.iterator().next()
    val versionAfterFirstUpdate = firstTaskResource.version
    val editDateAfterFirstUpdate = firstTaskResource.editDate
    assertThat(versionAfterFirstUpdate).isNotNull
    assertThat(editDateAfterFirstUpdate).isNotNull

    // Request again the same operation to validate that the version and edit date have not changed
    ReflectionTestUtils.setField(assignedDraftTask, "version", 1L)
    val secondUpdateTask = buildUpdateTaskBatchResource(assignedDraftTask)
    val secondResponse =
        taskController.updateTasks(UpdateBatchRequestResource(listOf(secondUpdateTask)))

    assertThat(secondResponse.statusCode).isEqualTo(OK)
    assertThat(secondResponse.body).isNotNull

    val secondTaskResource = secondResponse.body!!.tasks.iterator().next()
    assertThat(secondTaskResource.version).isEqualTo(versionAfterFirstUpdate)
    assertThat(secondTaskResource.editDate).isEqualTo(editDateAfterFirstUpdate)

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.UPDATED, 0, false)
  }

  @Test
  fun `verify assign task`() {
    val response =
        taskAssignmentController.assignTask(
            unassignedDraftTask.identifier,
            AssignTaskToParticipantResource(getIdentifier("participant").asParticipantId()))

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull

    // assigning is automatically followed by sending the task
    assertThatIsAssignedAndSent(setOf(response.body!!))

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.ASSIGNED, 1, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.SENT, 1, false)
  }

  @Test
  fun `verify assign task for closed task not possible`() {
    val exception =
        assertThrows(PreconditionViolationException::class.java) {
          taskAssignmentController.assignTask(
              closedTask.identifier,
              AssignTaskToParticipantResource(getIdentifier("participant").asParticipantId()))
        }

    assertThat(exception.messageKey)
        .isEqualTo(TASK_VALIDATION_ERROR_CLOSED_OR_ACCEPTED_TASK_ASSIGNMENT_FORBIDDEN)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify assign task for accepted task not possible`() {
    val exception =
        assertThrows(PreconditionViolationException::class.java) {
          taskAssignmentController.assignTask(
              acceptedTask.identifier,
              AssignTaskToParticipantResource(getIdentifier("participant").asParticipantId()))
        }

    assertThat(exception.messageKey)
        .isEqualTo(TASK_VALIDATION_ERROR_CLOSED_OR_ACCEPTED_TASK_ASSIGNMENT_FORBIDDEN)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify assign tasks`() {
    val assignTasksResource =
        AssignTaskListToParticipantResource(
            listOf(unassignedDraftTask.identifier.toUuid(), openTask.identifier.toUuid()),
            getIdentifier("participant").asParticipantId())

    val response = taskAssignmentController.assignTasks(assignTasksResource)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull

    // expect both tasks to be assigned and sent
    assertThat(response.body!!.tasks).hasSize(2)
    response.body!!.tasks.forEach { assertThatIsAssignedAndSent(setOf(it)) }

    // Only the unassignedDraftTask is assigned and sent
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.ASSIGNED, 1, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.SENT, 1, false)
  }

  @Test
  fun `verify assign 100 tasks`() {

    // Create 100 tasks
    val input = ArrayList<UUID>()
    for (i in 0..99) {
      eventStreamGenerator.submitTask("task$i") {
        it.assignee = null
        it.project = getByReference("project")
        it.status = TaskStatusEnumAvro.DRAFT
      }

      input.add(getIdentifier("task$i"))
    }

    // assign the 100 tasks
    val assignTasksResource =
        AssignTaskListToParticipantResource(input, getIdentifier("participant").asParticipantId())

    val response = taskAssignmentController.assignTasks(assignTasksResource)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull

    // expect tasks to be assigned and sent
    assertThat(response.body!!.tasks).hasSize(100)
    response.body!!.tasks.forEach { assertThatIsAssignedAndSent(setOf(it)) }

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.ASSIGNED, 100, false)
    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.SENT, 100, false)
  }

  @Test
  fun `verify assign 101 tasks fails`() {

    // Create 101 tasks
    val input = ArrayList<UUID>()
    for (i in 0..100) {
      eventStreamGenerator.submitTask("task$i") { it.assignee = getByReference("participant") }

      input.add(getIdentifier("task$i"))
    }

    val assignTasksResource =
        AssignTaskListToParticipantResource(input, getIdentifier("participant").asParticipantId())

    assertThatExceptionOfType(ConstraintViolationException::class.java)
        .isThrownBy { taskAssignmentController.assignTasks(assignTasksResource) }
        .withMessage("assignTasks.resource.taskIds: size must be between 1 and 100")

    projectEventStoreUtils.verifyEmpty()
  }

  private fun assertThatFieldsMatchAfterCreate(
      createTaskBatchResources: Collection<CreateTaskBatchResource>,
      taskResources: Collection<TaskResource>
  ) {
    assertThat(createTaskBatchResources.size).isEqualTo(taskResources.size)

    val taskResourceMap = taskResources.associateBy { it.id }

    createTaskBatchResources.forEach {
      assertThatFieldsMatchAfterCreate(it, taskResourceMap[it.id]!!)
    }
  }

  private fun assertThatFieldsMatchAfterCreate(
      saveTaskResource: SaveTaskResource,
      taskResource: TaskResource
  ) {

    taskResource.apply {
      assertThat(name).isEqualTo(saveTaskResource.name)
      assertThat(description).isEqualTo(saveTaskResource.description)
      assertThat(location).isEqualTo(saveTaskResource.location)
      assertThat(project.identifier.asProjectId()).isEqualTo(saveTaskResource.projectId)
      assertThat(workArea).isNull()
      assertThat(creator!!.identifier).isEqualTo(csmParticipant.identifier.toUuid())
      if (saveTaskResource.assigneeId == null) assertThat(editDate).isNull()
      else assertThat(editDate).isNotNull
    }
  }

  private fun assertThatFieldsMatchAfterUpdate(
      saveTaskResourceWithIdentifierAndVersions:
          Collection<SaveTaskResourceWithIdentifierAndVersion>,
      taskResources: Collection<TaskResource>
  ) {
    assertThat(saveTaskResourceWithIdentifierAndVersions.size).isEqualTo(taskResources.size)

    val taskResourceMap = taskResources.associateBy { it.id }

    saveTaskResourceWithIdentifierAndVersions.forEach {
      assertThatFieldsMatchAfterUpdate(it, taskResourceMap[it.id]!!)
    }
  }

  private fun assertThatFieldsMatchAfterUpdate(
      saveTaskResource: SaveTaskResource,
      taskResource: TaskResource
  ) {
    taskResource.apply {
      assertThat(name).isEqualTo(saveTaskResource.name)
      assertThat(description).isEqualTo(saveTaskResource.description)
      assertThat(location).isEqualTo(saveTaskResource.location)
      assertThat(project.identifier.asProjectId()).isEqualTo(saveTaskResource.projectId)
      assertThat(workArea).isNull()
      assertThat(creator!!.identifier).isEqualTo(csmParticipant.identifier.toUuid())
      assertThat(editDate).isNotNull
    }
  }

  private fun assertThatIsAssignedAndNotSent(taskResources: Collection<TaskResource>) {
    taskResources.forEach {
      assertThatIsAssigned(it)
      assertThat(it.status).isEqualTo(DRAFT)
    }
  }

  private fun assertThatIsAssignedAndSent(taskResources: Collection<TaskResource>) {
    taskResources.forEach {
      assertThatIsAssigned(it)
      assertThat(it.status).isEqualTo(OPEN)
    }
  }

  private fun assertThatIsAssigned(taskResource: TaskResource) {
    assertThat(taskResource.assignee).isNotNull
    assertThat(taskResource.assignee!!.identifier).isEqualTo(getIdentifier("participant"))

    assertThat(taskResource.assigned).isTrue

    assertThat(taskResource.company).isNotNull
    assertThat(taskResource.company!!.identifier).isEqualTo(getIdentifier("company"))
  }

  private fun assertThatIsUnassigned(taskResources: Collection<TaskResource>) {
    taskResources.forEach { this.assertThatIsUnassigned(it) }
  }

  private fun assertThatIsUnassigned(taskResource: TaskResource) {
    assertThat(taskResource.assignee).isNull()
    assertThat(taskResource.assigned).isFalse

    // assignee's company must be null
    assertThat(taskResource.company).isNull()

    // task must still be in state DRAFT (i.e. not yet "sent")
    assertThat(taskResource.status).isEqualTo(DRAFT)
  }

  private fun buildSaveTaskResource(assign: Boolean, send: Boolean): SaveTaskResource =
      SaveTaskResourceBuilder()
          .setProjectId(getIdentifier("project").asProjectId())
          .setProjectCraftId(getIdentifier("projectCraft").asProjectCraftId())
          .setName("name")
          .setDescription("description")
          .setLocation("location")
          .setStatus(if (assign && send) OPEN else DRAFT)
          .setAssigneeId(if (assign) getIdentifier("participant").asParticipantId() else null)
          .setWorkAreaId(null)
          .createSaveTaskResource()

  private fun buildCreateTaskBatchResource(
      assign: Boolean,
      send: Boolean
  ): CreateTaskBatchResource =
      CreateTaskBatchResource(
          randomUUID(),
          "name",
          "description",
          "location",
          if (assign && send) OPEN else DRAFT,
          getIdentifier("project").asProjectId(),
          getIdentifier("projectCraft").asProjectCraftId(),
          if (assign) getIdentifier("participant").asParticipantId() else null,
          null)

  private fun buildUpdateTaskBatchResource(task: Task): SaveTaskResourceWithIdentifierAndVersion =
      SaveTaskResourceWithIdentifierAndVersion(
          task.identifier.toUuid(),
          task.version,
          task.name,
          task.description,
          task.location,
          task.status,
          task.project.identifier,
          task.projectCraft.identifier,
          if (task.assignee == null) null else task.assignee!!.identifier,
          if (task.workArea == null) null else task.workArea!!.identifier)
}
