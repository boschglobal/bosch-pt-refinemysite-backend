/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.UpdateBatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.toEtag
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_NOT_OF_SAME_PROJECT
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_PARTICIPANT_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_WORK_AREA_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.SaveTaskResourceBuilder
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.SaveTaskResourceWithIdentifierAndVersion
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class TaskUpdateIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: TaskController

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  private val craft by lazy {
    repositories.findProjectCraft(getIdentifier("projectCraft").asProjectCraftId())!!
  }
  private val task by lazy { repositories.findTaskWithDetails(getIdentifier("task").asTaskId())!! }
  private val task2 by lazy {
    repositories.findTaskWithDetails(getIdentifier("task2").asTaskId())!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitTask(asReference = "task2") { it.assignee = getByReference("participant") }
        .submitTaskSchedule(asReference = "taskSchedule2")

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify update task with assignee not existing fails`() {
    val saveTaskResource =
        SaveTaskResourceBuilder()
            .setName("Update task")
            .setDescription("Update task description")
            .setLocation("Update location")
            .setProjectCraftId(craft.identifier)
            .setProjectId(projectIdentifier)
            .setAssigneeId(ParticipantId())
            .setStatus(OPEN)
            .createSaveTaskResource()

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.updateTask(task.identifier, saveTaskResource, task.toEtag()) }
        .withMessage(TASK_VALIDATION_ERROR_PARTICIPANT_NOT_FOUND)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update tasks with assignee not existing fails ( none of the tasks are created )`() {
    val saveTaskResources =
        listOf(
            SaveTaskResourceWithIdentifierAndVersion(
                task.identifier.toUuid(),
                task.version,
                "Update task",
                "Update task description",
                "Update location",
                OPEN,
                projectIdentifier,
                craft.identifier,
                task.assignee!!.identifier,
                null),
            SaveTaskResourceWithIdentifierAndVersion(
                task2.identifier.toUuid(),
                task2.version,
                "Update task",
                "Update task description",
                "Update location",
                OPEN,
                projectIdentifier,
                craft.identifier,
                ParticipantId(),
                null),
        )

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.updateTasks(UpdateBatchRequestResource(saveTaskResources)) }
        .withMessage(TASK_VALIDATION_ERROR_PARTICIPANT_NOT_FOUND)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update task with work area not existing fails`() {
    val saveTaskResource =
        SaveTaskResourceBuilder()
            .setName("Update task")
            .setDescription("Update task description")
            .setLocation("Update location")
            .setProjectCraftId(craft.identifier)
            .setProjectId(projectIdentifier)
            .setAssigneeId(task.assignee!!.identifier)
            .setStatus(OPEN)
            .setWorkAreaId(WorkAreaId())
            .createSaveTaskResource()

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.updateTask(task.identifier, saveTaskResource, task.toEtag()) }
        .withMessage(TASK_VALIDATION_ERROR_WORK_AREA_NOT_FOUND)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update tasks with work area not existing fails ( none of the tasks are created )`() {
    val saveTaskResources =
        listOf(
            SaveTaskResourceWithIdentifierAndVersion(
                task.identifier.toUuid(),
                task.version,
                "Update task",
                "Update task description",
                "Update location",
                OPEN,
                projectIdentifier,
                craft.identifier,
                task.assignee!!.identifier,
                null),
            SaveTaskResourceWithIdentifierAndVersion(
                task2.identifier.toUuid(),
                task2.version,
                "Update task",
                "Update task description",
                "Update location",
                OPEN,
                projectIdentifier,
                craft.identifier,
                task2.assignee!!.identifier,
                WorkAreaId()),
        )

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.updateTasks(UpdateBatchRequestResource(saveTaskResources)) }
        .withMessage(TASK_VALIDATION_ERROR_WORK_AREA_NOT_FOUND)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify update tasks with different projects fails ( none of the tasks are created )`() {
    val saveTaskResources =
        listOf(
            SaveTaskResourceWithIdentifierAndVersion(
                task.identifier.toUuid(),
                task.version,
                "Update task",
                "Update task description",
                "Update location",
                OPEN,
                projectIdentifier,
                craft.identifier,
                task.assignee!!.identifier,
                null),
            SaveTaskResourceWithIdentifierAndVersion(
                task2.identifier.toUuid(),
                task2.version,
                "Update task",
                "Update task description",
                "Update location",
                OPEN,
                ProjectId(),
                craft.identifier,
                task.assignee!!.identifier,
                null),
        )

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.updateTasks(UpdateBatchRequestResource(saveTaskResources)) }
        .withMessage(TASK_VALIDATION_ERROR_NOT_OF_SAME_PROJECT)

    projectEventStoreUtils.verifyEmpty()
  }
}
