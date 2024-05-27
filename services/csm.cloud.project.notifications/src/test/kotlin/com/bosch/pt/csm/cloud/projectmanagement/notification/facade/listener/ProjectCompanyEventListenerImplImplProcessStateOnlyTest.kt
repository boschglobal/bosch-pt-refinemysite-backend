/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.NotificationService
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.test.submitInitProjectData
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource

@DisplayName("When 'process state only' is enabled")
@SmartSiteSpringBootTest
@TestPropertySource(
    properties =
        [
            "custom.process-state-only.enabled=true",
            "custom.process-state-only.until-date=2021-01-15"])
class ProjectCompanyEventListenerImplImplProcessStateOnlyTest(
    @Autowired private val notificationService: NotificationService
) : BaseNotificationTest() {

  @Test
  fun `no notifications are created for message date before 'until date'`() {
    eventStreamGenerator.submitInitProjectData().submitTask(
            auditUserReference = CSM_USER,
            time = LocalDateTime.of(2021, 1, 14, 23, 59, 59, 0).toInstant(UTC)) {
      it.assignee = getByReference(FM_PARTICIPANT)
      it.status = TaskStatusEnumAvro.OPEN
    }

    val fmUserIdentifier = getIdentifier(FM_USER)

    assertThat(notificationService.findAll(fmUserIdentifier, Integer.MAX_VALUE))
        .extracting { it.resources?.size }
        .isEqualTo(0)
  }

  @Test
  fun `notifications are created for message date after 'until date'`() {
    eventStreamGenerator.submitInitProjectData().submitTask(
            auditUserReference = CSM_USER,
            time = LocalDateTime.of(2021, 1, 15, 0, 0, 0, 0).toInstant(UTC)) {
      it.assignee = getByReference(FM_PARTICIPANT)
      it.status = TaskStatusEnumAvro.OPEN
    }

    val fmUserIdentifier = getIdentifier(FM_USER)

    assertThat(notificationService.findAll(fmUserIdentifier, Integer.MAX_VALUE))
        .extracting { it.resources?.size }
        .isEqualTo(1)
  }
}
