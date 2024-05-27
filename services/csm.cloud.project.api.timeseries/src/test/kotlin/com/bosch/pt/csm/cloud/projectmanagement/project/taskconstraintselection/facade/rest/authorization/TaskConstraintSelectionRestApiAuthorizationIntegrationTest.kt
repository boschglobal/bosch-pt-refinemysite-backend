/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.rest.authorization

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAction
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.rest.TaskConstraintSelectionRestController.Companion.CONSTRAINTS_ENDPOINT
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.rest.resource.response.TaskConstraintSelectionListResource
import com.bosch.pt.csm.cloud.projectmanagement.taskaction.messages.TaskActionEnumAvro.COMMON_UNDERSTANDING
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.get

@RmsSpringBootTest
class TaskConstraintSelectionRestApiAuthorizationIntegrationTest :
    AbstractRestApiIntegrationTest() {

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitProject()
        .submitCsmParticipant()
        .submitProjectCraftG2()
        .submitTask()
        .submitTaskAction() { it.actions = listOf(COMMON_UNDERSTANDING) }
  }

  @Test
  fun `verify that an authorized user is allowed to read task constraint selections`() {
    setAuthentication("csm-user")
    val constraintList =
        query(
            latestProjectApi(CONSTRAINTS_ENDPOINT),
            false,
            TaskConstraintSelectionListResource::class.java)
    assertThat(constraintList.taskConstraints).isNotEmpty
  }

  @Test
  fun `verify that an unauthorized user is not allowed to read task constraint selections`() {
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
