/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(value = [SpringExtension::class])
@DisplayName("State must be updated upon a company event")
@SmartSiteSpringBootTest
class UpdateStateFromCompanyEventTest : BaseNotificationTest() {

  @Test
  fun `and company deleted from database`() {
    assertThat(getByReference(COMPANY)).isNotNull
    assertThat(
            repositories
                .companyRepository
                .findById(getByReference(COMPANY).toAggregateIdentifier())
                .get()
                .deleted)
        .isFalse
    eventStreamGenerator.submitCompany(eventType = DELETED)
    assertThat(getByReference(COMPANY)).isNotNull
    assertThat(
            repositories
                .companyRepository
                .findById(getByReference(COMPANY).toAggregateIdentifier())
                .get()
                .deleted)
        .isTrue
  }
}
