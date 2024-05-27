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
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTION_SELECTION_ACTIVITY_CREATED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAction
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro.COMMON_UNDERSTANDING
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro.CUSTOM1
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro.MATERIAL
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro.PRELIMINARY_WORK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@Suppress("ClassName")
@DisplayName("Verify Activity")
@SmartSiteSpringBootTest
class TaskConstraintSelectionCreatedActivityStrategyTest :
    AbstractTaskConstraintSelectionActivityStrategyTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setUserContext("fm-user").submitTask {
      it.assignee = getByReference("fm-participant")
      it.name = "task"
      it.status = DRAFT
      // setting all non-mandatory fields to null
      it.location = null
      it.description = null
    }
  }

  @ParameterizedTest
  @EnumSource(TaskConstraintEnum::class)
  fun `when task constraint selection is created`(constraint: TaskConstraintEnum) {
    eventStreamGenerator.submitTaskAction {
      it.actions = listOf(TaskActionEnumAvro.valueOf(constraint.name))
    }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary(TASK_ACTION_SELECTION_ACTIVITY_CREATED)))
        .andExpect(hasChange(translate(mapTaskConstraintToKey(constraint))))
  }

  @Test
  fun `when task constraint selection is created with multiple constraints with correct order`() {
    eventStreamGenerator.submitTaskAction {
      it.actions = listOf(COMMON_UNDERSTANDING, MATERIAL, CUSTOM1, PRELIMINARY_WORK)
    }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(hasDate(timeLineGenerator.time))
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary(TASK_ACTION_SELECTION_ACTIVITY_CREATED)))
        .andExpect(
            hasChange(
                text = translate(mapTaskConstraintToKey(TaskConstraintEnum.MATERIAL)),
                changeIndex = 0))
        .andExpect(
            hasChange(
                text = translate(mapTaskConstraintToKey(TaskConstraintEnum.PRELIMINARY_WORK)),
                changeIndex = 1))
        .andExpect(
            hasChange(
                text = translate(mapTaskConstraintToKey(TaskConstraintEnum.COMMON_UNDERSTANDING)),
                changeIndex = 2))
        .andExpect(
            hasChange(
                text = translate(mapTaskConstraintToKey(TaskConstraintEnum.CUSTOM1)),
                changeIndex = 3))
  }

  @Test
  fun `was not created when empty task constraint selection event is received`() {
    val existingActivities = repositories.activityRepository.findAll()
    eventStreamGenerator.submitTaskAction { it.actions = emptyList() }
    assertThat(repositories.activityRepository.findAll()).isEqualTo(existingActivities)
  }
}
