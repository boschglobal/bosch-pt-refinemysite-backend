/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.rest.authorization

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.rest.TaskConstraintRestController.Companion.CONSTRAINTS_ENDPOINT
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.rest.resource.response.TaskConstraintListResource
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.get

@RmsSpringBootTest
class TaskConstraintRestApiAuthorizationIntegrationTest : AbstractRestApiIntegrationTest() {

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitProject().submitCsmParticipant()
  }

  @Test
  fun `verify that an authorized user is allowed to read task constraints`() {
    setAuthentication("csm-user")
    val constraintList =
        query(latestProjectApi(CONSTRAINTS_ENDPOINT), false, TaskConstraintListResource::class.java)
    assertThat(constraintList.taskConstraints).isNotEmpty
  }

  @Test
  fun `verify that an unauthorized user is not allowed to read task constraints`() {
    eventStreamGenerator.submitUser("user2")
    setAuthentication("user2")
    mockMvc.get(latestProjectApi(CONSTRAINTS_ENDPOINT)).andExpect { status { isForbidden() } }
  }

  @Test
  fun `verify that an unauthenticated user is not allowed to read any data`() {
    TestSecurityContextHolder.clearContext()
    mockMvc.get(latestProjectApi(CONSTRAINTS_ENDPOINT)).andExpect { status { isUnauthorized() } }
  }
}
