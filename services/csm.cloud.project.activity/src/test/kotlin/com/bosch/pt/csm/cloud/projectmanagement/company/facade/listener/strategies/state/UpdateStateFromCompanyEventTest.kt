/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Verify company state")
@SmartSiteSpringBootTest
class UpdateStateFromCompanyEventTest : AbstractIntegrationTest() {

  @Test
  fun `is saved after company created event`() {
    assertThat(repositories.companyRepository.findAll()).hasSize(1)
    eventStreamGenerator.repeat { eventStreamGenerator.submitCompany(asReference = "new company") }
    assertThat(repositories.companyRepository.findAll()).hasSize(2)
  }

  @Test
  fun `is updated and cleaned up after company updated event`() {
    assertThat(repositories.companyRepository.findAll()).hasSize(1)

    eventStreamGenerator.repeat {
      eventStreamGenerator.submitCompany(eventType = UPDATED) { it.name = "update company" }
    }

    val companies = repositories.companyRepository.findAll()
    assertThat(companies).hasSize(1)
    assertThat(companies.first().identifier.identifier).isEqualTo(getIdentifier("company"))
    assertThat(companies.first().identifier.version).isEqualTo(1L)
  }
}
