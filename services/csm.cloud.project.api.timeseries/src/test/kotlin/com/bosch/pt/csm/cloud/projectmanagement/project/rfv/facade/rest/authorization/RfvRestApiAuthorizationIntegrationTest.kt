/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.rest.authorization

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRfvCustomization
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.rest.RfvRestController.Companion.RFVS_ENDPOINT
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.rest.resource.response.RfvListResource
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.CHANGED_PRIORITY
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.get

@RmsSpringBootTest
class RfvRestApiAuthorizationIntegrationTest : AbstractRestApiIntegrationTest() {

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitProject().submitCsmParticipant().submitRfvCustomization {
      it.key = CHANGED_PRIORITY
      it.name = "Important"
    }
  }

  @Test
  fun `verify that an authorized user is allowed to read rfvs`() {
    setAuthentication("csm-user")
    val rfvList = query(latestProjectApi(RFVS_ENDPOINT), false, RfvListResource::class.java)
    assertThat(rfvList.rfvs).isNotEmpty
  }

  @Test
  fun `verify that an unauthorized user is not allowed to read rfvs`() {
    eventStreamGenerator.submitUser("user2")
    setAuthentication("user2")
    mockMvc.get(latestProjectApi(RFVS_ENDPOINT)).andExpect { status { isForbidden() } }
  }

  @Test
  fun `verify that an unauthenticated user is not allowed to read any data`() {
    TestSecurityContextHolder.clearContext()
    mockMvc.get(latestProjectApi(RFVS_ENDPOINT)).andExpect { status { isUnauthorized() } }
  }
}
