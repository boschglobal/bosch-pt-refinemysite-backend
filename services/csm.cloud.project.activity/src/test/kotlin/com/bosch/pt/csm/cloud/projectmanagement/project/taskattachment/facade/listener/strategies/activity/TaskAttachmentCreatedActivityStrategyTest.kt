/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.ActivityMatchers
import com.bosch.pt.csm.cloud.projectmanagement.activity.andExpectOk
import com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.resources.response.SummaryDto
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractActivityIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.BARE_PARAMETERS_ONE_PARAMETER
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTACHMENT_ACTIVITY_SAVED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify task attachment")
@SmartSiteSpringBootTest
class TaskAttachmentCreatedActivityStrategyTest : AbstractActivityIntegrationTest() {

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

  @Test
  fun `created activity`() {
    eventStreamGenerator.submitTaskAttachment()

    requestActivities(task)
        .andExpectOk()
        .andExpect(ActivityMatchers.hasId(findLatestActivity().identifier))
        .andExpect(ActivityMatchers.hasDate(timeLineGenerator.time))
        .andExpect(ActivityMatchers.hasUser(fmUser))
        .andExpect(ActivityMatchers.hasSummary(buildSummary(TASK_ATTACHMENT_ACTIVITY_SAVED)))
        .andExpect(
            ActivityMatchers.hasChange(translate(BARE_PARAMETERS_ONE_PARAMETER, "myPicture.jpg")))
  }

  private fun buildSummary(messageKey: String, changesCount: Int = 1): SummaryDto {
    val objectReferences =
        mapOf(
            "originator" to
                buildPlaceholder(fmParticipant.getAggregateIdentifier(), displayName(fmUser)))

    return buildSummary(
        messageKey = messageKey,
        namedArguments = mapOf("count" to changesCount.toString()),
        objectReferences = objectReferences)
  }
}
