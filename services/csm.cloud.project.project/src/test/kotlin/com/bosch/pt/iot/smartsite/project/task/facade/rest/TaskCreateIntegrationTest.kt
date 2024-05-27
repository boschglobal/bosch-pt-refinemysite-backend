/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.CreateBatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_NOT_OF_SAME_PROJECT
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_PARTICIPANT_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.SaveTaskResourceBuilder
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.CreateTaskBatchResource
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class TaskCreateIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: TaskController

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify create task with assignee not existing fails`() {
    val saveTaskResource =
        SaveTaskResourceBuilder()
            .setName("What a task")
            .setDescription("Task description")
            .setLocation("location")
            .setProjectCraftId(getIdentifier("projectCraft").asProjectCraftId())
            .setWorkAreaId(getIdentifier("workArea").asWorkAreaId())
            .setProjectId(projectIdentifier)
            .setAssigneeId(ParticipantId())
            .setStatus(OPEN)
            .createSaveTaskResource()

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.createTask(TaskId(), saveTaskResource) }
        .withMessage(TASK_VALIDATION_ERROR_PARTICIPANT_NOT_FOUND)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify create tasks with assignee not existing fails ( none of the tasks are created )`() {
    val createTaskBatchResources =
        List(2) { index ->
          CreateTaskBatchResource(
              randomUUID(),
              "Task $index",
              "Task description $index",
              "location",
              OPEN,
              projectIdentifier,
              getIdentifier("projectCraft").asProjectCraftId(),
              ParticipantId(),
              getIdentifier("workArea").asWorkAreaId())
        }

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.createTasks(CreateBatchRequestResource(createTaskBatchResources)) }
        .withMessage(TASK_VALIDATION_ERROR_PARTICIPANT_NOT_FOUND)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify create tasks with different projects fails ( none of the tasks are created )`() {
    val createTaskBatchResources =
        List(2) { index ->
          CreateTaskBatchResource(
              randomUUID(),
              "Task $index",
              "Task description $index",
              "location",
              OPEN,
              ProjectId(),
              getIdentifier("projectCraft").asProjectCraftId(),
              ParticipantId(),
              getIdentifier("workArea").asWorkAreaId())
        }

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.createTasks(CreateBatchRequestResource(createTaskBatchResources)) }
        .withMessage(TASK_VALIDATION_ERROR_NOT_OF_SAME_PROJECT)

    projectEventStoreUtils.verifyEmpty()
  }
}
