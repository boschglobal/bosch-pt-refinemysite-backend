/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.facade.job

import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.simulateScheduledJob
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.testdata.invitationForUnregisteredUser
import com.bosch.pt.iot.smartsite.testdata.plainProjectWithCsm
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource

@EnableAllKafkaListeners
@TestPropertySource(properties = ["custom.mail.job.resend-invitation.enabled=false"])
class InvitationReminderJobDisabledTest : AbstractIntegrationTestV2() {

  @Autowired lateinit var cut: InvitationReminderJob

  @BeforeEach
  fun setupTestData() {
    timeLineGenerator.resetAtDaysAgo(28)
    eventStreamGenerator.submitSystemUserAndActivate().plainProjectWithCsm()
    timeLineGenerator.resetAtDaysAgo(7)
    eventStreamGenerator.invitationForUnregisteredUser()

    projectEventStoreUtils.reset()
    invitationEventStoreUtils.reset()
  }

  @Test
  @DisplayName("Job does nothing when disabled")
  fun doNothingWhenDisabled() {
    simulateScheduledJob { cut.sendFirstReminders() }
    invitationEventStoreUtils.verifyEmpty()
    projectEventStoreUtils.verifyEmpty()
  }
}
