/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasSummary
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers.hasUser
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectOk
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractActivityIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.DEACTIVATED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.util.displayName
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify Activity")
@SmartSiteSpringBootTest
class ActivityCreatedForActiveParticipantIntegrationTest : AbstractActivityIntegrationTest() {

  private val task by lazy { context["task"] as TaskAggregateAvro }

  @Test
  fun `created for an user with multiple participants`() {

    eventStreamGenerator
        .setUserContext("fm-user")
        .submitParticipantG3(asReference = "fm-participant", eventType = DEACTIVATED)
        .submitCompany(asReference = "other-company")
        .submitParticipantG3(asReference = "other-company-fm-participant") {
          it.user = getByReference("fm-user")
          it.company = getByReference("other-company")
        }
        .submitTask {
          it.assignee = getByReference("cr-participant")
          it.name = "task"
        }

    requestActivities(task)
        .andExpectOk()
        .andExpect(hasUser(fmUser))
        .andExpect(hasSummary(buildSummary()))
  }

  private fun buildSummary() =
      buildSummary(
          messageKey = TASK_ACTIVITY_CREATED,
          objectReferences =
              mapOf(
                  "originator" to
                      buildPlaceholder(
                          getByReference("other-company-fm-participant"), fmUser.displayName()),
                  "task" to buildPlaceholder(task.getAggregateIdentifier(), task.getName())))
}
