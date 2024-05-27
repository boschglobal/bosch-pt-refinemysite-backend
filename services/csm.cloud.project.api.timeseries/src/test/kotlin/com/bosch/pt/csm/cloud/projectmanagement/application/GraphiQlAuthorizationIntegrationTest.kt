/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application

import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.get

@RmsSpringBootTest
class GraphiQlAuthorizationIntegrationTest : AbstractRestApiIntegrationTest() {

  @Test
  fun `verify that graphiql is accessible without authorization`() {
    TestSecurityContextHolder.clearContext()

    mockMvc.get("/graphiql").andExpect { status { isTemporaryRedirect() } }
  }
}
