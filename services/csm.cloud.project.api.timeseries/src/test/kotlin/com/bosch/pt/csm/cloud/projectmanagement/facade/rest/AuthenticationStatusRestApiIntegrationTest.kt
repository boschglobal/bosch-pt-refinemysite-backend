/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.facade.rest

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get

@RmsSpringBootTest
class AuthenticationStatusRestApiIntegrationTest : AbstractRestApiIntegrationTest() {

  @Test
  fun `authentication status is true for authorized users`() {
    setAuthentication("csm-user")
    mockMvc.get(latestAuthenticationStatusApi("")).andExpectAll {
      status { isOk() }
      content { json("{ authenticated: true}") }
    }
  }

  @Test
  fun `authentication status is unauthorized for unauthorized users`() {
    mockMvc.get(latestAuthenticationStatusApi("")).andExpect { status { isUnauthorized() } }
  }
}
