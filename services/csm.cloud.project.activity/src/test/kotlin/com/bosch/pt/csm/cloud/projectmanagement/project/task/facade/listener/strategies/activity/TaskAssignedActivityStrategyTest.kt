/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
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
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.ASSIGNED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import com.bosch.pt.csm.cloud.projectmanagement.util.displayName
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify Activity")
@SmartSiteSpringBootTest
class TaskAssignedActivityStrategyTest : AbstractActivityIntegrationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }

  @Test
  fun `when a task is assigned`() {
    eventStreamGenerator.setUserContext("cr-user").submitTask().submitTask(eventType = ASSIGNED) {
      it.assignee = getByReference("fm-participant")
      it.status = DRAFT
    }

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
          Key.TASK_ACTIVITY_ASSIGNED,
          objectReferences =
              mapOf(
                  "originator" to
                      buildPlaceholder(
                          crParticipant.getAggregateIdentifier(), crUser.displayName()),
                  "assignee" to
                      buildPlaceholder(
                          fmParticipant.getAggregateIdentifier(), fmUser.displayName())))
}
