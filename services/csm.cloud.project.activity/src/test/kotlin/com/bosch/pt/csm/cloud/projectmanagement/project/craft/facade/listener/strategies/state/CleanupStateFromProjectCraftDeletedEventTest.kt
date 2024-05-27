/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.craft.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify project state")
@SmartSiteSpringBootTest
class CleanupStateFromProjectCraftDeletedEventTest : AbstractIntegrationTest() {

  @Test
  fun `is cleaned up after projectCraft deleted event`() {
    assertThat(findProjectCraft()).isNotNull
    eventStreamGenerator.submitProjectCraftG2(asReference = "projectCraft1", eventType = DELETED)
    assertThat(findProjectCraft()).isNull()
  }

  private fun findProjectCraft() =
      repositories.projectCraftRepository.findLatest(
          projectCraft1.getIdentifier(), project.getIdentifier())
}
