/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.feature.facade.rest

import com.bosch.pt.csm.cloud.common.AbstractApiDocumentationTest
import com.bosch.pt.csm.cloud.featuretoggle.feature.facade.rest.resource.request.CreateFeatureResource
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.facade.rest.resource.request.CreateWhitelistedSubjectResource
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class FeatureApiAuthorizationIntegrationTest : AbstractApiDocumentationTest() {

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitUserAndActivate("admin") { it.locale = "de_DE" }
        .submitUser("user1") { it.admin = false }

    setAuthentication("user1")
  }

  @Test
  fun `non-admin users are not permitted to create a feature`() {
    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf("/features")),
                content = CreateFeatureResource("projectImport")))
        .andExpectAll(status().isForbidden)
  }

  @Test
  fun `non-admin users are not permitted to delete a feature`() {

    mockMvc
        .perform(requestBuilder(delete(latestVersionOf("/features/someFeature"))))
        .andExpectAll(status().isForbidden)
  }

  @Test
  fun `non-admin users are not permitted to disable a feature`() {

    mockMvc
        .perform(requestBuilder(post(latestVersionOf("/features/projectImport/disable"))))
        .andExpectAll(status().isForbidden)
  }

  @Test
  fun `non-admin users are not permitted to enable a feature`() {

    mockMvc
        .perform(requestBuilder(post(latestVersionOf("/features/projectImport/enable"))))
        .andExpectAll(status().isForbidden)
  }

  @Test
  fun `non-admin users are not permitted to activate the whitelist of a feature`() {

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf("/features/projectImport/activate-whitelist"))))
        .andExpectAll(status().isForbidden)
  }

  @Test
  fun `non-non-admin users are not permitted to add subjects to the whitelist of a feature`() {

    mockMvc
        .perform(
            requestBuilder(
                put(
                    latestVersionOf(
                        "/features/projectImport/subjects/5db09bc5-68f2-48e7-b04a-68dc49e7ef56")),
                content = CreateWhitelistedSubjectResource(SubjectTypeEnum.COMPANY)))
        .andExpectAll(status().isForbidden)
  }

  @Test
  fun `non-admin users are not permitted to remove subjects from the whitelist of a feature`() {

    mockMvc
        .perform(
            requestBuilder(
                delete(
                    latestVersionOf(
                        "/features/projectImport/subjects/5db09bc5-68f2-48e7-b04a-68dc49e7ef56"))))
        .andExpectAll(status().isForbidden)
  }
}
