/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest

import com.bosch.pt.csm.cloud.common.command.messages.DeleteCommandAvro
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.model.key.CommandMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASK
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.kafka.AggregateIdentifierUtils.getAggregateIdentifier
import com.bosch.pt.iot.smartsite.common.kafka.messaging.CommandSendingService
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.TaskAttachmentListResource
import com.ninjasquad.springmockk.MockkBean
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK

@EnableAllKafkaListeners
class TaskFindAndDeletionIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var taskController: TaskController

  @MockkBean(relaxed = true) private lateinit var commandSendingService: CommandSendingService

  private val csmParticipant: Participant by lazy {
    repositories.findParticipant(getIdentifier("participantCsm"))!!
  }

  private val openTask: Task by lazy {
    repositories.findTaskWithDetails(getIdentifier("taskOpen").asTaskId())!!
  }

  private val task: Task by lazy {
    repositories.findTaskWithDetails(getIdentifier("task").asTaskId())!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitUser("userCsm")
        .submitEmployee("employeeCsm") { it.roles = listOf(EmployeeRoleEnumAvro.CSM) }
        .submitParticipantG3("participantCsm") { it.role = ParticipantRoleEnumAvro.CSM }
        .submitUser("userFmInactive")
        .submitEmployee("employeeFmInactive") { it.roles = listOf(EmployeeRoleEnumAvro.FM) }
        .submitParticipantG3("participantFmInactive") {
          it.role = ParticipantRoleEnumAvro.FM
          it.status = ParticipantStatusEnumAvro.INACTIVE
        }
        .setUserContext("userCsm")
        .submitTask("unassignedDraftTask") {
          it.assignee = null
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask("assignedDraftTask") {
          it.assignee = getByReference("participant")
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask("assignedDraftTaskWithInactiveParticipant") {
          it.assignee = getByReference("participantFmInactive")
          it.status = TaskStatusEnumAvro.DRAFT
        }
        .submitTask("taskOpen") {
          it.assignee = getByReference("participant")
          it.status = TaskStatusEnumAvro.OPEN
          it.editDate = null
        }

    setAuthentication(getIdentifier("userCsm"))

    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify find task`() {
    val response = taskController.findOneById(openTask.identifier)

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull
    assertThatFieldsMatch(openTask, response.body!!)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify find task with attachments`() {
    eventStreamGenerator.submitTaskAttachment { it.task = getByReference("taskOpen") }

    val response = taskController.findOneById(openTask.identifier)
    val taskResource = response.body!!

    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body).isNotNull
    assertThatFieldsMatch(openTask, response.body!!)

    val attachments =
        taskResource.embeddedResources[TaskResource.EMBEDDED_TASK_ATTACHMENTS]
            as TaskAttachmentListResource

    assertThat(attachments).isNotNull
    assertThat(attachments.attachments).hasSize(1)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify delete task`() {
    val response = taskController.deleteTask(task.identifier)
    assertThat(response.statusCode).isEqualTo(NO_CONTENT)
    assertThat(repositories.findTaskWithDetails(task.identifier)!!.deleted).isTrue

    verify {
      commandSendingService.send(
          CommandMessageKey(task.project.identifier.toUuid()), buildDeleteCommand(task), any())
    }
  }

  @Test
  fun `verify delete multiple tasks`() {
    val deleteTaskBatchResource =
        BatchRequestResource(setOf(task.identifier.toUuid(), openTask.identifier.toUuid()))

    val response =
        taskController.deleteTasks(projectId = task.project.identifier, deleteTaskBatchResource)
    assertThat(response.statusCode).isEqualTo(NO_CONTENT)

    assertThat(repositories.findTaskWithDetails(task.identifier)!!.deleted).isTrue
    assertThat(repositories.findTaskWithDetails(openTask.identifier)!!.deleted).isTrue

    verify {
      commandSendingService.send(
          CommandMessageKey(task.project.identifier.toUuid()), buildDeleteCommand(task), any())
      commandSendingService.send(
          CommandMessageKey(openTask.project.identifier.toUuid()),
          buildDeleteCommand(openTask),
          any())
    }
  }

  private fun assertThatFieldsMatch(task: Task, taskResource: TaskResource) {
    taskResource.apply {
      assertThat(name).isEqualTo(task.name)
      assertThat(description).isEqualTo(task.description)
      assertThat(location).isEqualTo(task.location)
      assertThat(status).isEqualTo(task.status)
      assertThat(assigned).isEqualTo(task.isAssigned())
      assertThat(project.identifier.asProjectId()).isEqualTo(task.project.identifier)
      assertThat(creator!!.identifier).isEqualTo(csmParticipant.identifier.toUuid())
      assertThat(editDate).isNull()
      assertThat(projectCraft.id.asProjectCraftId()).isEqualTo(task.projectCraft.identifier)
    }

    if (task.assignee != null) {
      assertThat(taskResource.assignee)
          .extracting { it!!.identifier }
          .isEqualTo(task.assignee!!.identifier.toUuid())
      assertThat(taskResource.company)
          .extracting { it!!.identifier }
          .isEqualTo(task.assignee!!.company!!.identifier)
    } else {
      assertThat(taskResource.assignee).isNull()
      assertThat(taskResource.company).isNull()
    }

    if (task.workArea != null) {
      assertThat(taskResource.workArea)
          .extracting { it!!.identifier }
          .isEqualTo(task.workArea!!.identifier)
    } else {
      assertThat(taskResource.workArea).isNull()
    }
  }

  private fun buildDeleteCommand(task: Task): DeleteCommandAvro =
      DeleteCommandAvro(
          getAggregateIdentifier(task, TASK.value),
          getAggregateIdentifier(SecurityContextHelper.getInstance().getCurrentUser(), USER.value))
}
