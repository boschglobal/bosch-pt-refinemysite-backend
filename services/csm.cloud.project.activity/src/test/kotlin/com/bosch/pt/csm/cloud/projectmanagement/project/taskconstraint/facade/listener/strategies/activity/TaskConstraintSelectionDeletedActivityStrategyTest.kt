/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasChange
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasDate
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasId
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasSummary
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasUser
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectOk
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTION_SELECTION_ACTIVITY_ACTIONS_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTION_SELECTION_ACTIVITY_REMOVED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAction
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro.MATERIAL
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro.RESOURCES
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.UPDATED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@Suppress("ClassName")
@DisplayName("Verify Activity")
@SmartSiteSpringBootTest
class TaskConstraintSelectionDeletedActivityStrategyTest :
    AbstractTaskConstraintSelectionActivityStrategyTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setUserContext("fm-user")
        .submitTask {
          it.assignee = getByReference("fm-participant")
          it.name = "task"
          it.status = DRAFT
          // setting all non-mandatory fields to null
          it.location = null
          it.description = null
        }
        .submitTaskAction { it.actions = emptyList() }
  }

  @Test
  fun `when task constraint selection with a single constraint is deleted`() {
    eventStreamGenerator
        .submitTaskAction(eventType = UPDATED) { it.actions = listOf(MATERIAL) }
        .submitTaskAction(eventType = DELETED) { it.actions = listOf(MATERIAL) }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary(TASK_ACTION_SELECTION_ACTIVITY_ACTIONS_UPDATED)))
        .andExpect(
            hasChange(
                translate(
                    TASK_ACTION_SELECTION_ACTIVITY_REMOVED,
                    translate(mapTaskConstraintToKey(TaskConstraintEnum.MATERIAL)))))
  }

  @Test
  fun `when task constraint selection with multiple constraints is deleted`() {
    eventStreamGenerator
        .submitTaskAction(eventType = UPDATED) { it.actions = listOf(RESOURCES, MATERIAL) }
        .submitTaskAction(eventType = DELETED) { it.actions = listOf(RESOURCES, MATERIAL) }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary(TASK_ACTION_SELECTION_ACTIVITY_ACTIONS_UPDATED)))
        .andExpect(
            hasChange(
                text =
                    translate(
                        TASK_ACTION_SELECTION_ACTIVITY_REMOVED,
                        translate(mapTaskConstraintToKey(TaskConstraintEnum.RESOURCES))),
                changeIndex = 0))
        .andExpect(
            hasChange(
                text =
                    translate(
                        TASK_ACTION_SELECTION_ACTIVITY_REMOVED,
                        translate(mapTaskConstraintToKey(TaskConstraintEnum.MATERIAL))),
                changeIndex = 1))
  }

  @Test
  fun `when task constraint selection with an empty list of constraints is deleted`() {
    val lastActivityIdentifier = findLatestActivity().identifier
    eventStreamGenerator.submitTaskAction(eventType = DELETED) { it.actions = emptyList() }
    assertThat(findLatestActivity().identifier).isEqualTo(lastActivityIdentifier)
  }
}
