/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.relation.facade.rest.authorization

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRelation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.facade.rest.RelationRestController.Companion.RELATIONS_ENDPOINT
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.facade.rest.resource.response.RelationListResource
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationTypeEnumAvro.FINISH_TO_START
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.get

@RmsSpringBootTest
class RelationRestApiAuthorizationIntegrationTest : AbstractRestApiIntegrationTest() {

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .submitProject()
        .submitCsmParticipant()
        .submitProjectCraftG2()
        .submitTask("task1")
        .submitTaskSchedule { it.task = getByReference("task1") }
        .submitTask("task2")
        .submitTaskSchedule { it.task = getByReference("task2") }
        .submitRelation {
          it.source = getByReference("task1")
          it.target = getByReference("task2")
          it.type = FINISH_TO_START
        }
  }

  @Test
  fun `verify that an authorized user is allowed to read relations`() {
    setAuthentication("csm-user")
    val relationList =
        query(latestProjectApi(RELATIONS_ENDPOINT), false, RelationListResource::class.java)
    assertThat(relationList.relations).isNotEmpty
  }

  @Test
  fun `verify that an unauthorized user is not allowed to read relations`() {
    eventStreamGenerator.submitUser("user2")
    setAuthentication("user2")
    mockMvc.get(latestProjectApi(RELATIONS_ENDPOINT)).andExpect { status { isForbidden() } }
  }

  @Test
  fun `verify that an unauthenticated user is not allowed to read any data`() {
    TestSecurityContextHolder.clearContext()
    mockMvc.get(latestProjectApi(RELATIONS_ENDPOINT)).andExpect { status { isUnauthorized() } }
  }
}
