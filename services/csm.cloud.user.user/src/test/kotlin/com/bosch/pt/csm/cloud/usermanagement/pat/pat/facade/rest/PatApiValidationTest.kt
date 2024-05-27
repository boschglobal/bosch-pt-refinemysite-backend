/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest

import com.bosch.pt.csm.cloud.usermanagement.common.AbstractApiDocumentationTest
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.request.CreateOrUpdatePatResource
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.PatScopeEnum
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class PatApiValidationTest : AbstractApiDocumentationTest() {

  @BeforeEach
  fun setup() {
    eventStreamGenerator.submitUserAndActivate("user")
    setAuthentication("user")
  }

  @Test
  fun `verify that create PAT fails for validity of over 365 days`() {
    val createPatResource =
        CreateOrUpdatePatResource(
            description = "PAT that cannot be valid for that long",
            scopes = listOf(PatScopeEnum.GRAPHQL_API_READ),
            validForMinutes = (24 * 60 * 366),
        )
    mockMvc
        .perform(requestBuilder(post(latestVersionOf("/users/current/pats")), createPatResource))
        .andExpect(MockMvcResultMatchers.status().isBadRequest)
  }

  @Test
  fun `verify that create PAT fails for validity under 1 minute`() {
    val createPatResource =
        CreateOrUpdatePatResource(
            description = "PAT that cannot be valid for that short a time",
            scopes = listOf(PatScopeEnum.GRAPHQL_API_READ),
            validForMinutes = (0),
        )
    mockMvc
        .perform(requestBuilder(post(latestVersionOf("/users/current/pats")), createPatResource))
        .andExpect(MockMvcResultMatchers.status().isBadRequest)
  }

  @Test
  fun `verify that create PAT fails with an empty scope list`() {
    val createPatResource =
        CreateOrUpdatePatResource(
            description = "PAT that cannot be valid with 0 scopes",
            scopes = listOf(),
            validForMinutes = (24 * 60),
        )
    mockMvc
        .perform(requestBuilder(post(latestVersionOf("/users/current/pats")), createPatResource))
        .andExpect(MockMvcResultMatchers.status().isBadRequest)
  }
}
