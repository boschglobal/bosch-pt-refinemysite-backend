/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.command.handler

import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.iot.smartsite.common.authorization.AbstractAuthorizationIntegrationTestV2
import org.junit.jupiter.api.BeforeEach

abstract class AbstractMessageAuthorizationIntegrationTest :
    AbstractAuthorizationIntegrationTestV2() {

  @BeforeEach
  fun init() {
    // Only the csm users can add project crafts to a project
    eventStreamGenerator
        .setUserContext("userCsm")
        .submitProjectCraftG2()
        .setUserContext("userCreator")
        .submitTask()
        .submitTopicG2()
        .submitMessage()
  }
}
