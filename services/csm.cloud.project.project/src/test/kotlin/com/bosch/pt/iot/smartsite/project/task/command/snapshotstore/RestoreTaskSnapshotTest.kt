/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.snapshotstore

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreStrategyTest
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAction
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getWorkAreaIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.ACCEPTED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.ASSIGNED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.CLOSED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.RESET
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.SENT
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.STARTED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.UNASSIGNED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.OPEN
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro.EQUIPMENT
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.util.TimeUtilities
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RestoreStrategyTest
open class RestoreTaskSnapshotTest : AbstractRestoreIntegrationTestV2() {

  private val task by lazy { repositories.findTaskWithDetails(getIdentifier("task").asTaskId())!! }
  private val taskAggregate by lazy {
    EventStreamGeneratorStaticExtensions.get<TaskAggregateAvro>("task")!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()
  }

  @Test
  fun `validate that task created event was processed successfully`() {
    validateTaskAttributes(task, taskAggregate)
  }

  @Test
  fun `validate that task updated event was processed successfully`() {
    eventStreamGenerator.submitTask(eventType = UPDATED) { it.name = "updated task" }

    assertThat(task.name).isEqualTo("updated task")
    validateTaskAttributes(task, taskAggregate)
  }

  @Test
  fun `validate that task assigned event was processed successfully`() {
    eventStreamGenerator.submitTask(eventType = ASSIGNED) {
      it.assignee = EventStreamGeneratorStaticExtensions.getByReference("participant")
    }

    assertThat(task.assignee).isNotNull
    assertThat(task.assignee!!.identifier).isEqualTo(getIdentifier("participant").asParticipantId())
    validateTaskAttributes(task, taskAggregate)
  }

  @Test
  fun `validate that task sent event was processed successfully`() {
    eventStreamGenerator.submitTask(eventType = SENT) { it.status = OPEN }

    assertThat(task.status).isEqualTo(TaskStatusEnum.OPEN)
    validateTaskAttributes(task, taskAggregate)
  }

  @Test
  fun `validate that task started event was processed successfully`() {
    eventStreamGenerator.submitTask(eventType = STARTED) { it.status = TaskStatusEnumAvro.STARTED }

    assertThat(task.status).isEqualTo(TaskStatusEnum.STARTED)
    validateTaskAttributes(task, taskAggregate)
  }

  @Test
  fun `validate that task unassigned event was processed successfully`() {
    eventStreamGenerator.submitTask(eventType = UNASSIGNED) {
      it.status = OPEN
      it.assignee = null
    }

    assertThat(task.status).isEqualTo(TaskStatusEnum.OPEN)
    assertThat(task.assignee).isNull()
    validateTaskAttributes(task, taskAggregate)
  }

  @Test
  fun `validate that task closed event was processed successfully`() {
    eventStreamGenerator.submitTask(eventType = CLOSED) { it.status = TaskStatusEnumAvro.CLOSED }

    assertThat(task.status).isEqualTo(TaskStatusEnum.CLOSED)
    validateTaskAttributes(task, taskAggregate)
  }

  @Test
  fun `validate that task accepted event was processed successfully`() {
    eventStreamGenerator.submitTask(eventType = ACCEPTED) {
      it.status = TaskStatusEnumAvro.ACCEPTED
    }

    assertThat(task.status).isEqualTo(TaskStatusEnum.ACCEPTED)
    validateTaskAttributes(task, taskAggregate)
  }

  @Test
  fun `validate task deleted event deletes a task and its data`() {
    eventStreamGenerator
        .submitTaskAction { it.actions = listOf(EQUIPMENT) }
        .submitTaskSchedule()
        .submitDayCardG2()
        .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED)
        .submitTopicG2()
        .submitMessage()
        .submitTaskAttachment()
        .submitTopicAttachment()
        .submitMessageAttachment()

    validateRepositoryObjectCounts(1)

    eventStreamGenerator.submitTask(eventType = DELETED)

    validateRepositoryObjectCounts(0)

    // Send event again to test idempotency
    eventStreamGenerator.repeat(1)
  }

  @Test
  fun `validate that task reset event was processed successfully`() {
    eventStreamGenerator.submitTask(eventType = RESET) { it.status = OPEN }

    assertThat(task.status).isEqualTo(TaskStatusEnum.OPEN)
    validateTaskAttributes(task, taskAggregate)
  }

  private fun validateRepositoryObjectCounts(expectedCount: Int) {
    assertThat(repositories.taskRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.taskConstraintSelectionRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.taskScheduleRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.dayCardRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.topicRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.messageRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.taskAttachmentRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.topicAttachmentRepository.findAll()).hasSize(expectedCount)
    assertThat(repositories.messageAttachmentRepository.findAll()).hasSize(expectedCount)
  }

  private fun validateTaskAttributes(task: Task, taskAggregate: TaskAggregateAvro) {
    validateAuditingInformationAndIdentifierAndVersion(task, taskAggregate)

    assertThat(task.description).isEqualTo(taskAggregate.description)
    assertThat(task.location).isEqualTo(taskAggregate.location)
    assertThat(task.name).isEqualTo(taskAggregate.name)
    assertThat(task.project.identifier)
        .isEqualTo(taskAggregate.getProjectIdentifier().asProjectId())
    assertThat(task.projectCraft.identifier.toUuid())
        .isEqualTo(taskAggregate.craft.identifier.toUUID())
    assertThat(task.status.name).isEqualTo(taskAggregate.status.name)

    if (taskAggregate.assignee != null) {
      assertThat(task.assignee!!.identifier)
          .isEqualTo(taskAggregate.assignee.identifier.toUUID().asParticipantId())
    }
    if (taskAggregate.editDate != null) {
      assertThat(task.editDate?.toLocalDate())
          .isEqualTo(TimeUtilities.asLocalDate(taskAggregate.editDate))
    }
    if (taskAggregate.workarea != null) {
      assertThat(task.workArea!!.identifier.toUuid())
          .isEqualTo(taskAggregate.getWorkAreaIdentifier())
    }
  }
}
