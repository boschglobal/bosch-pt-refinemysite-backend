/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.rest.authorization

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.rest.CompanyRestController.Companion.COMPANIES_ENDPOINT
import com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.rest.resource.response.CompanyListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.get

@RmsSpringBootTest
class CompanyRestApiAuthorizationIntegrationTest : AbstractRestApiIntegrationTest() {

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitProject().submitCsmParticipant()
  }

  @Test
  fun `verify that an authorized user is allowed to read project companies`() {
    setAuthentication("csm-user")
    val companyList =
        query(latestCompanyApi(COMPANIES_ENDPOINT), false, CompanyListResource::class.java)
    assertThat(companyList.companies).isNotEmpty
  }

  @Test
  fun `verify that an unauthorized user is not allowed to read project companies`() {
    eventStreamGenerator.submitUser("user2")
    setAuthentication("user2")
    mockMvc.get(latestCompanyApi(COMPANIES_ENDPOINT)).andExpect { status { isForbidden() } }
  }

  @Test
  fun `verify that an unauthenticated user is not allowed to read any data`() {
    TestSecurityContextHolder.clearContext()
    mockMvc.get(latestCompanyApi(COMPANIES_ENDPOINT)).andExpect { status { isUnauthorized() } }
  }
}
