/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify project state")
@SmartSiteSpringBootTest
class CleanupStateFromMilestoneDeletedEventTest : AbstractIntegrationTest() {

  @Test
  fun `is cleaned up after milestone deleted event`() {
    assertThat(findMilestone()).isNotNull
    eventStreamGenerator.submitMilestone(asReference = "milestone1", eventType = DELETED)
    assertThat(findMilestone()).isNull()
  }

  private fun findMilestone() =
      repositories.milestoneRepository.findLatest(
          milestone1.getIdentifier(), project.getIdentifier())
}
