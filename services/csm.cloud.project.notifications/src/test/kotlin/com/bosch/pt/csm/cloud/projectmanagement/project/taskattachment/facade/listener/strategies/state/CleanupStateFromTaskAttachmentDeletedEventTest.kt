/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.submitTaskAsFm
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro.DELETED
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify task attachment state")
@SmartSiteSpringBootTest
class CleanupStateFromTaskAttachmentDeletedEventTest : BaseNotificationStrategyTest() {

  @Test
  fun ` is cleaned up from task attachment deleted event`() {
    eventStreamGenerator.submitTaskAsFm().submitTaskAttachment(auditUserReference = FM_USER)

    var taskAttachments =
        repositories.taskAttachmentRepository.findTaskAttachments(projectAggregate.getIdentifier())
    Assertions.assertThat(taskAttachments).hasSize(1)

    eventStreamGenerator.repeat { eventStreamGenerator.submitTaskAttachment(eventType = DELETED) }
    taskAttachments =
        repositories.taskAttachmentRepository.findTaskAttachments(projectAggregate.getIdentifier())
    Assertions.assertThat(taskAttachments).isEmpty()
  }
}
