/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.rest

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAction
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.rest.resource.response.TaskConstraintSelectionListResource
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class TaskConstraintSelectionRestApiIntegrationTest : AbstractRestApiIntegrationTest() {

  @BeforeEach
  fun init() {
    submitBaseEvents()
    setAuthentication("csm-user")
  }

  @Test
  fun `query empty task constraint selection`() {

    // Execute query
    val taskConstraintSelectionList: TaskConstraintSelectionListResource = query(false)

    // Verify no constraint selections exists
    assertThat(taskConstraintSelectionList.taskConstraints).hasSize(0)
  }

  @Test
  fun `query task constraint selection`() {

    submitEvents()

    val task = eventStreamGenerator.get<TaskAggregateAvro>("task")!!
    // val anotherTask = eventStreamGenerator.get<TaskAggregateAvro>("anotherTask")!!

    // Execute query
    val taskConstraintSelectionList: TaskConstraintSelectionListResource = query(false)

    assertThat(taskConstraintSelectionList.taskConstraints).hasSize(5)

    // Created event
    assertThat(taskConstraintSelectionList.taskConstraints[0].key)
        .isEqualTo(TaskConstraintEnum.INFORMATION.key)
    assertThat(taskConstraintSelectionList.taskConstraints[0].task.value.toString())
        .isEqualTo(task.aggregateIdentifier.identifier)
    assertThat(taskConstraintSelectionList.taskConstraints[0].deleted).isEqualTo(false)

    assertThat(taskConstraintSelectionList.taskConstraints[1].key)
        .isEqualTo(TaskConstraintEnum.SAFE_WORKING_ENVIRONMENT.key)
    assertThat(taskConstraintSelectionList.taskConstraints[1].task.value.toString())
        .isEqualTo(task.aggregateIdentifier.identifier)
    assertThat(taskConstraintSelectionList.taskConstraints[1].deleted).isEqualTo(false)

    // Updated event
    assertThat(taskConstraintSelectionList.taskConstraints[2].key)
        .isEqualTo(TaskConstraintEnum.COMMON_UNDERSTANDING.key)
    assertThat(taskConstraintSelectionList.taskConstraints[2].task.value.toString())
        .isEqualTo(task.aggregateIdentifier.identifier)
    assertThat(taskConstraintSelectionList.taskConstraints[2].deleted).isEqualTo(false)

    assertThat(taskConstraintSelectionList.taskConstraints[3].key)
        .isEqualTo(TaskConstraintEnum.INFORMATION.key)
    assertThat(taskConstraintSelectionList.taskConstraints[3].deleted).isEqualTo(false)

    assertThat(taskConstraintSelectionList.taskConstraints[4].key)
        .isEqualTo(TaskConstraintEnum.SAFE_WORKING_ENVIRONMENT.key)
    assertThat(taskConstraintSelectionList.taskConstraints[4].deleted).isEqualTo(false)
  }

  @Test
  fun `query deleted task constraint selection`() {
    submitEvents().submitTaskAction(eventType = TaskActionSelectionEventEnumAvro.DELETED)

    // Execute query
    val taskConstraintSelectionList: TaskConstraintSelectionListResource = query(false)

    assertThat(taskConstraintSelectionList.taskConstraints).hasSize(8)

    // Updated event
    assertThat(taskConstraintSelectionList.taskConstraints[2].key)
        .isEqualTo(TaskConstraintEnum.COMMON_UNDERSTANDING.key)
    assertThat(taskConstraintSelectionList.taskConstraints[2].deleted).isEqualTo(false)

    assertThat(taskConstraintSelectionList.taskConstraints[3].key)
        .isEqualTo(TaskConstraintEnum.INFORMATION.key)
    assertThat(taskConstraintSelectionList.taskConstraints[3].deleted).isEqualTo(false)

    assertThat(taskConstraintSelectionList.taskConstraints[4].key)
        .isEqualTo(TaskConstraintEnum.SAFE_WORKING_ENVIRONMENT.key)
    assertThat(taskConstraintSelectionList.taskConstraints[4].deleted).isEqualTo(false)

    // Deleted event
    assertThat(taskConstraintSelectionList.taskConstraints[5].key)
        .isEqualTo(TaskConstraintEnum.COMMON_UNDERSTANDING.key)
    assertThat(taskConstraintSelectionList.taskConstraints[5].deleted).isEqualTo(true)

    assertThat(taskConstraintSelectionList.taskConstraints[6].key)
        .isEqualTo(TaskConstraintEnum.INFORMATION.key)
    assertThat(taskConstraintSelectionList.taskConstraints[6].deleted).isEqualTo(true)

    assertThat(taskConstraintSelectionList.taskConstraints[7].key)
        .isEqualTo(TaskConstraintEnum.SAFE_WORKING_ENVIRONMENT.key)
    assertThat(taskConstraintSelectionList.taskConstraints[7].deleted).isEqualTo(true)
  }

  @Test
  fun `query deleted task constraint selection latest only`() {
    submitEvents().submitTaskAction(eventType = TaskActionSelectionEventEnumAvro.DELETED)

    // Execute query
    val taskConstraintSelectionList: TaskConstraintSelectionListResource = query(true)

    assertThat(taskConstraintSelectionList.taskConstraints).isEmpty()
  }

  @Test
  fun `query task constraint selection from multiple tasks`() {

    submitEvents().submitTask("anotherTask").submitTaskAction("anotherTaskAction")

    val task = eventStreamGenerator.get<TaskAggregateAvro>("task")!!
    val anotherTask = eventStreamGenerator.get<TaskAggregateAvro>("anotherTask")!!

    // Execute query
    val taskConstraintSelectionList: TaskConstraintSelectionListResource = query(false)

    // Verify no constraint selections exists
    assertThat(taskConstraintSelectionList.taskConstraints).hasSize(6)

    assertThat(
            taskConstraintSelectionList.taskConstraints.filter {
              it.task.value.toString() == task.aggregateIdentifier.identifier
            })
        .hasSize(5)
    assertThat(
            taskConstraintSelectionList.taskConstraints.filter {
              it.task.value.toString() == anotherTask.aggregateIdentifier.identifier
            })
        .hasSize(1)
  }

  @Test
  fun `query task constraint selection - latest only`() {

    submitEvents()

    // Execute query
    val taskConstraintSelectionList: TaskConstraintSelectionListResource = query(true)

    assertThat(taskConstraintSelectionList.taskConstraints).hasSize(3)

    assertThat(taskConstraintSelectionList.taskConstraints[0].key)
        .isEqualTo(TaskConstraintEnum.COMMON_UNDERSTANDING.key)
    assertThat(taskConstraintSelectionList.taskConstraints[1].key)
        .isEqualTo(TaskConstraintEnum.INFORMATION.key)
    assertThat(taskConstraintSelectionList.taskConstraints[2].key)
        .isEqualTo(TaskConstraintEnum.SAFE_WORKING_ENVIRONMENT.key)
  }

  private fun query(latestOnly: Boolean) =
      super.query(
          latestProjectApi("/projects/tasks/constraints"),
          latestOnly,
          TaskConstraintSelectionListResource::class.java)

  private fun submitBaseEvents() =
      eventStreamGenerator.submitProject().submitCsmParticipant().submitProjectCraftG2()

  private fun submitEvents() =
      eventStreamGenerator
          .submitTask()
          .submitTaskAction() {
            it.actions =
                listOf(TaskActionEnumAvro.SAFE_WORKING_ENVIRONMENT, TaskActionEnumAvro.INFORMATION)
          }
          .submitTaskAction(eventType = TaskActionSelectionEventEnumAvro.UPDATED) {
            it.actions =
                listOf(
                    TaskActionEnumAvro.SAFE_WORKING_ENVIRONMENT,
                    TaskActionEnumAvro.INFORMATION,
                    TaskActionEnumAvro.COMMON_UNDERSTANDING)
          }
}
