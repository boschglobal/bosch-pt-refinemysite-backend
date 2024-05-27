/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.command.handler

import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicCriticalityEnumAvro
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import org.junit.jupiter.api.BeforeEach

abstract class AbstractTopicAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @BeforeEach
  fun init() {
    eventStreamGenerator
        // Only the csm users can add project crafts to a project
        .setUserContext("userCsm")
        .submitProjectCraftG2()
        .setUserContext("userCreator")
        .submitTask()
        .submitTopicG2(asReference = "criticalTopic") {
          it.criticality = TopicCriticalityEnumAvro.CRITICAL
        }
        .submitTopicG2(asReference = "uncriticalTopic") {
          it.criticality = TopicCriticalityEnumAvro.UNCRITICAL
        }
  }
}
