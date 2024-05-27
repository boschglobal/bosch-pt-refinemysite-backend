/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasDate
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasId
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasNoChanges
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasSummary
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasUser
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectOk
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractActivityIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getLastModifiedDate
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.RESET
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.CLOSED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.OPEN
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.STARTED
import com.bosch.pt.csm.cloud.projectmanagement.util.displayName
import org.junit.jupiter.api.Test

@SmartSiteSpringBootTest
class TaskResetActivityStrategyTest : AbstractActivityIntegrationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }

  @Test
  fun `verify reset a task when task status is started`() {
    eventStreamGenerator
        .setUserContext("cr-user")
        .submitTask {
          it.assignee = getByReference("fm-participant")
          it.status = STARTED
        }
        .submitTask(eventType = RESET) { task -> task.status = OPEN }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(
            hasDate(eventStreamGenerator.get<TaskAggregateAvro>("task")!!.getLastModifiedDate()))
        .andExpect(hasUser(crUser))
        .andExpect(hasSummary(buildSummary()))
        .andExpect(hasNoChanges())
  }

  @Test
  fun `verify reset a task when task status is closed`() {
    eventStreamGenerator
        .setUserContext("cr-user")
        .submitTask {
          it.assignee = getByReference("fm-participant")
          it.status = CLOSED
        }
        .submitTask(eventType = RESET) { task -> task.status = OPEN }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasId(findLatestActivity().identifier))
        .andExpect(
            hasDate(eventStreamGenerator.get<TaskAggregateAvro>("task")!!.getLastModifiedDate()))
        .andExpect(hasUser(crUser))
        .andExpect(hasSummary(buildSummary()))
        .andExpect(hasNoChanges())
  }

  private fun buildSummary() =
      buildSummary(
          Key.TASK_ACTIVITY_RESET,
          objectReferences =
              mapOf(
                  "originator" to
                      buildPlaceholder(
                          crParticipant.getAggregateIdentifier(), crUser.displayName())))
}
