/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest.taskstatus

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.doWithAuthorization
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_ACCEPTED_TASK_ACCEPT_FORBIDDEN
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_CLOSED_OR_ACCEPTED_TASK_CLOSE_FORBIDDEN
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_DRAFT_OR_OPEN_TASK_RESET_FORBIDDEN
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_OPEN_POSSIBLE_WHEN_STATUS_DRAFT
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_PARTICIPANT_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_START_ONLY_POSSIBLE_WHEN_STATUS_IS_DRAFT_OR_OPEN
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.CLOSED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.STARTED
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.util.withMessageKey
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.OK
import org.springframework.security.access.AccessDeniedException

@EnableAllKafkaListeners
class TaskStatusIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var taskStatusController: TaskStatusController

  private val assignedDraftTask: Task by lazy {
    repositories.findTaskWithDetails(getIdentifier("assignedDraftTask").asTaskId())!!
  }

  private val unassignedDraftTask: Task by lazy {
    repositories.findTaskWithDetails(getIdentifier("unassignedDraftTask").asTaskId())!!
  }

  private val assignedDraftTaskWithInactiveParticipant: Task by lazy {
    repositories.findTaskWithDetails(
        getIdentifier("assignedDraftTaskWithInactiveParticipant").asTaskId())!!
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

  private val userCr: User by lazy { repositories.findUser(getIdentifier("userCr"))!! }

  private val userFm: User by lazy { repositories.findUser(getIdentifier("userFm"))!! }

  private val userCsm: User by lazy { repositories.findUser(getIdentifier("userCsm"))!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitUser("userCsm")
        .submitParticipantG3("participantCsm") { it.role = ParticipantRoleEnumAvro.CSM }
        .submitUser("userFmInactive")
        .submitParticipantG3("participantFmInactive") {
          it.role = ParticipantRoleEnumAvro.FM
          it.status = ParticipantStatusEnumAvro.INACTIVE
        }
        .submitTask("assignedDraftTask") {
          it.assignee = getByReference("participant")
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask("unassignedDraftTask") {
          it.assignee = null
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask("assignedDraftTaskWithInactiveParticipant") {
          it.assignee = getByReference("participantFmInactive")
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
        .submitUser("userCr")
        .submitParticipantG3("participantCr") { it.role = ParticipantRoleEnumAvro.CR }
        .submitUser("userFm")
        .submitParticipantG3("participantFm") { it.role = ParticipantRoleEnumAvro.FM }

    setAuthentication(getIdentifier("userCsm"))

    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify start task for open task as user`() {
    val user = openTask.assignee!!.user

    doWithAuthorization(user) {
      val response = taskStatusController.startTask(openTask.identifier)
      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body).isNotNull

      projectEventStoreUtils.verifyContains(
          TaskEventAvro::class.java, TaskEventEnumAvro.STARTED, 1, true)
    }
  }

  @Test
  fun `verify start task for draft task`() {
    val response = taskStatusController.startTask(assignedDraftTask.identifier)

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull
    assertThat(response.body!!.status).isEqualTo(STARTED)
    assertThat(response.body!!.editDate).isNotNull

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.STARTED, 1, true)
  }

  @Test
  fun `verify start task for started task not possible`() {
    assertThatThrownBy { taskStatusController.startTask(startedTask.identifier) }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(
            PreconditionViolationException(
                TASK_VALIDATION_ERROR_START_ONLY_POSSIBLE_WHEN_STATUS_IS_DRAFT_OR_OPEN))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify start task for closed task not possible`() {
    assertThatThrownBy { taskStatusController.startTask(closedTask.identifier) }
        .usingRecursiveComparison()
        .withStrictTypeChecking()
        .isEqualTo(
            PreconditionViolationException(
                TASK_VALIDATION_ERROR_START_ONLY_POSSIBLE_WHEN_STATUS_IS_DRAFT_OR_OPEN))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify close task for draft task`() {
    val response = taskStatusController.closeTask(assignedDraftTask.identifier)

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull
    assertThat(response.body!!.status).isEqualTo(CLOSED)
    assertThat(response.body!!.editDate).isNotNull

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.CLOSED, 1, true)
  }

  @Test
  fun `verify close task for open task`() {
    val response = taskStatusController.closeTask(openTask.identifier)

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull
    assertThat(response.body!!.status).isEqualTo(CLOSED)
    assertThat(response.body!!.editDate).isNotNull

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.CLOSED, 1, true)
  }

  @Test
  fun `verify close task for started task`() {
    val response = taskStatusController.closeTask(startedTask.identifier)

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull
    assertThat(response.body!!.status).isEqualTo(CLOSED)
    assertThat(response.body!!.editDate).isNotNull

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.CLOSED, 1, true)
  }

  @Test
  fun `verify close task for closed task not possible`() {
    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { taskStatusController.closeTask(closedTask.identifier) }
        .withMessageKey(TASK_VALIDATION_ERROR_CLOSED_OR_ACCEPTED_TASK_CLOSE_FORBIDDEN)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify reset task for draft task not possible`() {
    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { taskStatusController.resetTask(assignedDraftTask.identifier) }
        .withMessageKey(TASK_VALIDATION_ERROR_DRAFT_OR_OPEN_TASK_RESET_FORBIDDEN)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify reset task for open task not possible`() {
    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { taskStatusController.resetTask(openTask.identifier) }
        .withMessageKey(TASK_VALIDATION_ERROR_DRAFT_OR_OPEN_TASK_RESET_FORBIDDEN)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify reset task for started task`() {
    val response = taskStatusController.resetTask(startedTask.identifier)

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull
    assertThat(response.body!!.status).isEqualTo(OPEN)
    assertThat(response.body!!.editDate).isNotNull

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.RESET, 1, true)
  }

  @Test
  fun `verify reset task for closed task`() {
    val response = taskStatusController.resetTask(closedTask.identifier)

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull
    assertThat(response.body!!.status).isEqualTo(OPEN)
    assertThat(response.body!!.editDate).isNotNull

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.RESET, 1, true)
  }

  @Test
  fun `verify send task for draft task`() {
    val response = taskStatusController.sendTask(assignedDraftTask.identifier)

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull
    assertThatIsAssignedAndSent(setOf(response.body!!))

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.SENT, 1, true)
  }

  @Test
  fun `verify send task for draft task unassigned`() {
    val response = taskStatusController.sendTask(unassignedDraftTask.identifier)

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull
    assertThatIsSent(setOf(response.body!!))

    projectEventStoreUtils.verifyContains(
        TaskEventAvro::class.java, TaskEventEnumAvro.SENT, 1, true)
  }

  @Test
  fun `verify send task for open task not possible`() {
    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { taskStatusController.sendTask(openTask.identifier) }
        .withMessageKey(TASK_VALIDATION_ERROR_OPEN_POSSIBLE_WHEN_STATUS_DRAFT)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify send task with inactive participant not possible`() {
    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy {
          taskStatusController.sendTask(assignedDraftTaskWithInactiveParticipant.identifier)
        }
        .withMessageKey(TASK_VALIDATION_ERROR_PARTICIPANT_NOT_FOUND)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify send draft task as FM not possible if not assigned`() {
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      doWithAuthorization(userFm) { taskStatusController.startTask(assignedDraftTask.identifier) }
    }

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify start draft task as CR is not possible`() {
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      doWithAuthorization(userCr) { taskStatusController.startTask(assignedDraftTask.identifier) }
    }

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify start draft task as FM is not possible`() {
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      doWithAuthorization(userFm) { taskStatusController.startTask(assignedDraftTask.identifier) }
    }

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify close draft task as CR is not possible`() {
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      doWithAuthorization(userCr) { taskStatusController.closeTask(assignedDraftTask.identifier) }
    }

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify close draft task as FM is not possible`() {
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      doWithAuthorization(userFm) { taskStatusController.closeTask(assignedDraftTask.identifier) }
    }

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify accept task for draft task as super indent`() {
    doWithAuthorization(userCsm) {
      val response = taskStatusController.acceptTask(assignedDraftTask.identifier)
      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body).isNotNull

      projectEventStoreUtils.verifyContains(
          TaskEventAvro::class.java, TaskEventEnumAvro.ACCEPTED, 1, true)
    }
  }

  @Test
  fun `verify accept task for open task as super indent`() {
    doWithAuthorization(userCsm) {
      val response = taskStatusController.acceptTask(openTask.identifier)
      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body).isNotNull

      projectEventStoreUtils.verifyContains(
          TaskEventAvro::class.java, TaskEventEnumAvro.ACCEPTED, 1, true)
    }
  }

  @Test
  fun `verify accept task for started task as super indent`() {
    doWithAuthorization(userCsm) {
      val response = taskStatusController.acceptTask(startedTask.identifier)
      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body).isNotNull

      projectEventStoreUtils.verifyContains(
          TaskEventAvro::class.java, TaskEventEnumAvro.ACCEPTED, 1, true)
    }
  }

  @Test
  fun `verify accept task for closed task as super indent`() {
    doWithAuthorization(userCsm) {
      val response = taskStatusController.acceptTask(closedTask.identifier)
      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(response.body).isNotNull

      projectEventStoreUtils.verifyContains(
          TaskEventAvro::class.java, TaskEventEnumAvro.ACCEPTED, 1, true)
    }
  }

  @Test
  fun `verify accept an accepted task with not possible`() {
    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { taskStatusController.acceptTask(acceptedTask.identifier) }
        .withMessageKey(TASK_VALIDATION_ERROR_ACCEPTED_TASK_ACCEPT_FORBIDDEN)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify accept draft task as CR is not possible`() {
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      doWithAuthorization(userCr) { taskStatusController.acceptTask(assignedDraftTask.identifier) }
    }

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify accept draft task as FM is not possible`() {
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      doWithAuthorization(userFm) { taskStatusController.acceptTask(assignedDraftTask.identifier) }
    }

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify accept open task as CR is not possible`() {
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      doWithAuthorization(userCr) { taskStatusController.acceptTask(openTask.identifier) }
    }

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify accept open task as FM is not possible`() {
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      doWithAuthorization(userFm) { taskStatusController.acceptTask(openTask.identifier) }
    }

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify accept started task as CR is not possible`() {
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      doWithAuthorization(userCr) { taskStatusController.acceptTask(startedTask.identifier) }
    }

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify accept started task as FM is not possible`() {
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      doWithAuthorization(userFm) { taskStatusController.acceptTask(startedTask.identifier) }
    }

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify accept closed task as CR is not possible`() {
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      doWithAuthorization(userCr) { taskStatusController.acceptTask(closedTask.identifier) }
    }

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify accept closed task as FM is not possible`() {
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      doWithAuthorization(userFm) { taskStatusController.acceptTask(closedTask.identifier) }
    }

    projectEventStoreUtils.verifyEmpty()
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

  private fun assertThatIsSent(taskResources: Collection<TaskResource>) {
    taskResources.forEach { assertThat(it.status).isEqualTo(OPEN) }
  }
}
