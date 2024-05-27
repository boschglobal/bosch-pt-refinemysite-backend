/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.attachment.authorization

import com.bosch.pt.csm.cloud.common.blob.model.BoundedContext
import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractAuthorizationIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.attachment.service.AttachmentService
import com.bosch.pt.csm.cloud.projectmanagement.common.exceptions.ResourceNotFoundException
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import com.bosch.pt.csm.cloud.projectmanagement.util.doWithAuthorization
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Verify User")
@SmartSiteSpringBootTest
class AttachmentServiceAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTest() {

  @Autowired lateinit var cut: AttachmentService

  @BeforeEach
  fun init() {
    eventStreamGenerator.setUserContext("fm-user").submitTask {
      it.assignee = getByReference("fm-participant")
      it.name = "task"
      it.status = DRAFT
      // setting all non-mandatory fields to null
      it.location = null
      it.description = null
    }
  }

  @TestFactory
  fun `permission to generate blob url`() =
      checkAccessWith(activeProjectParticipants()) {
        cut.generateBlobAccessUrl(
            BoundedContext.PROJECT, getIdentifier("task"), randomUUID(), ImageResolution.ORIGINAL)
      }

  @Test
  fun `is denied when task does not exist`() {
    assertThatExceptionOfType(ResourceNotFoundException::class.java).isThrownBy {
      doWithAuthorization(repositories.findUser(fmUser)) {
        cut.generateBlobAccessUrl(
            BoundedContext.PROJECT, randomUUID(), randomUUID(), ImageResolution.ORIGINAL)
      }
    }
  }
}
