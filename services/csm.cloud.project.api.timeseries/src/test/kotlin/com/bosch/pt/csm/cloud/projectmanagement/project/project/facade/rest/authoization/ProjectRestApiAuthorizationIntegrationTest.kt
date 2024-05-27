/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.rest.authoization

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.rest.ProjectRestController.Companion.PROJECTS_ENDPOINT
import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.rest.resource.response.ProjectListResource
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.get

@RmsSpringBootTest
class ProjectRestApiAuthorizationIntegrationTest : AbstractRestApiIntegrationTest() {

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitProject().submitCsmParticipant()
  }

  @Test
  fun `verify that an authorized user is allowed to read projects`() {
    setAuthentication("csm-user")
    val projectList =
        query(latestProjectApi(PROJECTS_ENDPOINT), false, ProjectListResource::class.java)
    assertThat(projectList.projects).isNotEmpty
  }

  @Test
  fun `verify that an unauthorized user is not allowed to read projects`() {
    eventStreamGenerator.submitUser("user2")
    setAuthentication("user2")
    mockMvc.get(latestProjectApi(PROJECTS_ENDPOINT)).andExpect { status { isForbidden() } }
  }

  @Test
  fun `verify that an unauthenticated user is not allowed to read any data`() {
    TestSecurityContextHolder.clearContext()
    mockMvc.get(latestProjectApi(PROJECTS_ENDPOINT)).andExpect { status { isUnauthorized() } }
  }
}
