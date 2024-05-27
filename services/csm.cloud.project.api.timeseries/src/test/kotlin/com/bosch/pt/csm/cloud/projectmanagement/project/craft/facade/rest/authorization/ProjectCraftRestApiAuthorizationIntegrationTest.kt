/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.rest.authorization

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.rest.ProjectCraftRestController.Companion.PROJECT_CRAFTS_ENDPOINT
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.rest.resource.response.ProjectCraftListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.get

@RmsSpringBootTest
class ProjectCraftRestApiAuthorizationIntegrationTest : AbstractRestApiIntegrationTest() {

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitProject().submitCsmParticipant().submitProjectCraftG2()
  }

  @Test
  fun `verify that an authorized user is allowed to read project crafts`() {
    setAuthentication("csm-user")
    val craftList =
        query(
            latestProjectApi(PROJECT_CRAFTS_ENDPOINT), false, ProjectCraftListResource::class.java)
    assertThat(craftList.crafts).isNotEmpty
  }

  @Test
  fun `verify that an unauthorized user is not allowed to read project crafts`() {
    eventStreamGenerator.submitUser("user2")
    setAuthentication("user2")
    mockMvc.get(latestProjectApi(PROJECT_CRAFTS_ENDPOINT)).andExpect { status { isForbidden() } }
  }

  @Test
  fun `verify that an unauthenticated user is not allowed to read any data`() {
    TestSecurityContextHolder.clearContext()
    mockMvc.get(latestProjectApi(PROJECT_CRAFTS_ENDPOINT)).andExpect { status { isUnauthorized() } }
  }
}
