/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.facade.listener.strategies.notifications

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationStrategyTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * We only test one assignment scenario since the recipient determination is always the same and
 * already tested with other types of notifications.
 */
@DisplayName("When an attachment was added to a draft task ")
@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@SmartSiteSpringBootTest
class TaskAttachmentCreatedForDraftTaskTest : BaseNotificationStrategyTest() {

  @Test
  fun `nobody is notified`() {
    eventStreamGenerator.submitTask(auditUserReference = FM_USER) {
      it.assignee = getByReference(FM_PARTICIPANT)
      it.status = TaskStatusEnumAvro.DRAFT
    }
    repositories.notificationRepository.deleteAll()

    eventStreamGenerator.submitTaskAttachment()

    assertThat(repositories.notificationRepository.findAll()).isEmpty()
  }
}
