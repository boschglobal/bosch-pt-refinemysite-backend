/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.boundary

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class TopicRequestDeleteServiceAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @Autowired private lateinit var cut: TopicRequestDeleteService

  private val topic by lazy { repositories.findTopic(getIdentifier("topic").asTopicId())!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        // Only the csm users can add project crafts to a project
        .setUserContext("userCsm")
        .submitProjectCraftG2()
        .setUserContext("userCreator")
        .submitTask()
        .submitTopicG2()
  }

  @ParameterizedTest
  @MethodSource("csmAndCrAndCreatorWithAccess")
  fun `verify delete topic authorized`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.markAsDeletedAndSendEvent(topic.identifier) }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `verify delete topic not authorized non existing topic`(userType: UserTypeAccess) =
      checkAccessWith(userType) { cut.markAsDeletedAndSendEvent(TopicId()) }
}
