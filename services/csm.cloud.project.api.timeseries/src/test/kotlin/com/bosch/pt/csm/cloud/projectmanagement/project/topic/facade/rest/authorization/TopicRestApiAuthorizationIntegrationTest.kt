/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.rest.authorization

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.rest.TopicRestController.Companion.TOPICS_ENDPOINT
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.rest.resource.response.TopicListResource
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.get

@RmsSpringBootTest
class TopicRestApiAuthorizationIntegrationTest : AbstractRestApiIntegrationTest() {

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitProject()
        .submitCsmParticipant()
        .submitProjectCraftG2()
        .submitTask()
        .submitTopicG2()
  }

  @Test
  fun `verify that an authorized user is allowed to read topics`() {
    setAuthentication("csm-user")
    val topicList = query(latestProjectApi(TOPICS_ENDPOINT), false, TopicListResource::class.java)
    assertThat(topicList.topics).isNotEmpty
  }

  @Test
  fun `verify that an unauthorized user is not allowed to read topics`() {
    eventStreamGenerator.submitUser("user2")
    setAuthentication("user2")
    mockMvc.get(latestProjectApi(TOPICS_ENDPOINT)).andExpect { status { isForbidden() } }
  }

  @Test
  fun `verify that an unauthenticated user is not allowed to read any data`() {
    TestSecurityContextHolder.clearContext()
    mockMvc.get(latestProjectApi(TOPICS_ENDPOINT)).andExpect { status { isUnauthorized() } }
  }
}
