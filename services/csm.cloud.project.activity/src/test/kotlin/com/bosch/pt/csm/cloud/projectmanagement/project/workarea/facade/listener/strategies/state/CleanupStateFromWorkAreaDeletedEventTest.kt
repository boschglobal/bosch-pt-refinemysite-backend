/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.workarea.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro.DELETED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify project state")
@SmartSiteSpringBootTest
class CleanupStateFromWorkAreaDeletedEventTest : AbstractIntegrationTest() {

  @Test
  fun `is cleaned up after workArea deleted event`() {
    assertThat(findWorkArea()).isNotNull
    eventStreamGenerator.submitWorkArea(asReference = "workArea1", eventType = DELETED)
    assertThat(findWorkArea()).isNull()
  }

  private fun findWorkArea() =
      repositories.workAreaRepository.findLatest(workArea1.getIdentifier(), project.getIdentifier())
}
