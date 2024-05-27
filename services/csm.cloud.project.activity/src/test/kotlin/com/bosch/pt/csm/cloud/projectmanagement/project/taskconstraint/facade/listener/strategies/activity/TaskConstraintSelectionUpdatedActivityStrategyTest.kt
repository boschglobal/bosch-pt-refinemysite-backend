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
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTION_SELECTION_ACTIVITY_ADDED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTION_SELECTION_ACTIVITY_REMOVED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAction
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro.MATERIAL
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro.RESOURCES
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionSelectionEventEnumAvro.UPDATED
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@Suppress("ClassName")
@DisplayName("Verify Activity")
@SmartSiteSpringBootTest
class TaskConstraintSelectionUpdatedActivityStrategyTest :
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

  @ParameterizedTest
  @EnumSource(TaskConstraintEnum::class)
  fun `when Task Constraint Selection is added`(constraint: TaskConstraintEnum) {
    eventStreamGenerator.submitTaskAction(eventType = UPDATED) {
      it.actions = listOf(TaskActionEnumAvro.valueOf(constraint.name))
    }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary(TASK_ACTION_SELECTION_ACTIVITY_ACTIONS_UPDATED)))
        .andExpect(
            hasChange(
                translate(
                    TASK_ACTION_SELECTION_ACTIVITY_ADDED,
                    translate(mapTaskConstraintToKey(constraint)))))
  }

  @Test
  fun `when Task Constraint Selection is added and removed`() {
    eventStreamGenerator
        .submitTaskAction(eventType = UPDATED) { it.actions = listOf(RESOURCES) }
        .submitTaskAction(eventType = UPDATED) { it.actions = listOf(MATERIAL) }

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
                        TASK_ACTION_SELECTION_ACTIVITY_ADDED,
                        translate(mapTaskConstraintToKey(TaskConstraintEnum.MATERIAL))),
                changeIndex = 1))
  }
}
