/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.activity.authorization

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.activity.service.ActivityService
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractAuthorizationIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.exceptions.ResourceNotFoundException
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import com.bosch.pt.csm.cloud.projectmanagement.util.doWithAuthorization
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Verify User")
@SmartSiteSpringBootTest
class ActivityServiceAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTest() {

  @Autowired lateinit var cut: ActivityService

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setUserContext("fm-user")
        .submitTask {
          it.assignee = getByReference("fm-participant")
          it.name = "task"
          it.status = DRAFT
          // setting all non-mandatory fields to null
          it.location = null
          it.description = null
        }
        .submitTopicG2 { it.description = "topic" }
  }

  @TestFactory
  fun `permission to read task activities`() =
      checkAccessWith(activeProjectParticipants()) { cut.findAll(getIdentifier("task"), null, 1) }

  @TestFactory
  fun `permission to read activity by attachmentIdentifier`(): List<DynamicTest> {
    eventStreamGenerator.submitMessage().submitMessageAttachment()

    return checkAccessWith(activeProjectParticipants()) {
      cut.findActivityByAttachmentIdentifier(getIdentifier("messageAttachment"))
    }
  }

  @Test
  fun `is denied when task does not exist`() {
    assertThatExceptionOfType(ResourceNotFoundException::class.java).isThrownBy {
      doWithAuthorization(repositories.findUser(fmUser)) { cut.findAll(randomUUID(), null, 1) }
    }
  }

  @Test
  fun `is denied when attachment does not exist`() {
    assertThatExceptionOfType(ResourceNotFoundException::class.java).isThrownBy {
      doWithAuthorization(repositories.findUser(fmUser)) {
        cut.findActivityByAttachmentIdentifier(attachmentId = randomUUID())
      }
    }
  }
}
