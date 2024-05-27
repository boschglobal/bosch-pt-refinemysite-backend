/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.rest.authorization

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.rest.ParticipantRestController.Companion.PARTICIPANTS_ENDPOINT
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.rest.resource.response.ParticipantListResource
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.get

@RmsSpringBootTest
class ParticipantRestApiAuthorizationIntegrationTest : AbstractRestApiIntegrationTest() {

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitProject().submitCsmParticipant()
  }

  @Test
  fun `verify that an authorized user is allowed to read participants`() {
    setAuthentication("csm-user")
    val participantList =
        query(latestProjectApi(PARTICIPANTS_ENDPOINT), false, ParticipantListResource::class.java)
    assertThat(participantList.participants).isNotEmpty
  }

  @Test
  fun `verify that an unauthorized user is not allowed to read participants`() {
    eventStreamGenerator.submitUser("user2")
    setAuthentication("user2")
    mockMvc.get(latestProjectApi(PARTICIPANTS_ENDPOINT)).andExpect { status { isForbidden() } }
  }

  @Test
  fun `verify that an unauthenticated user is not allowed to read any data`() {
    TestSecurityContextHolder.clearContext()
    mockMvc.get(latestProjectApi(PARTICIPANTS_ENDPOINT)).andExpect { status { isUnauthorized() } }
  }
}
