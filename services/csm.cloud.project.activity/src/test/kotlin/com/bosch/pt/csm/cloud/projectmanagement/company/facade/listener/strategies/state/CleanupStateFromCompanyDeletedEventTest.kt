/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify company state")
@SmartSiteSpringBootTest
class CleanupStateFromCompanyDeletedEventTest : AbstractIntegrationTest() {

  @Test
  fun `is cleaned up after company deleted event`() {
    assertThat(repositories.companyRepository.findAll()).isNotEmpty
    eventStreamGenerator.submitCompany(eventType = DELETED)
    assertThat(repositories.companyRepository.findAll()).isEmpty()
  }
}
