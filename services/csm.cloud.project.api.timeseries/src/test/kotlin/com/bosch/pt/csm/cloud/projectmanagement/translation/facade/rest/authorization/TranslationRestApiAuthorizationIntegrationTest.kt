/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.translation.facade.rest.authorization

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.translation.facade.rest.resource.response.TranslationListResource
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.get

@RmsSpringBootTest
class TranslationRestApiAuthorizationIntegrationTest : AbstractRestApiIntegrationTest() {

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitProject().submitCsmParticipant()
  }

  @Test
  fun `verify that project participants are allowed to read translations`() {
    setAuthentication("csm-user")
    val translationList =
        query(
            latestTranslationApi("/translations"),
            false,
            TranslationListResource::class.java)
    assertThat(translationList.translations).isNotEmpty
  }

  @Test
  fun `verify that non-project participants are allowed to read translations`() {
    eventStreamGenerator.submitUser("user2")
    setAuthentication("user2")
    val translationList =
        query(
            latestTranslationApi("/translations"),
            false,
            TranslationListResource::class.java)
    assertThat(translationList.translations).isNotEmpty
  }

  @Test
  fun `verify that an unauthenticated user is not allowed to read any data`() {
    TestSecurityContextHolder.clearContext()
    mockMvc.get(latestTranslationApi("/translations")).andExpect {
      status { isUnauthorized() }
    }
  }
}
