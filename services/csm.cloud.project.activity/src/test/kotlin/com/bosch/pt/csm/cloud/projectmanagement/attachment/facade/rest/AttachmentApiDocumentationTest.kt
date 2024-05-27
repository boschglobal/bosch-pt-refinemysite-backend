/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.attachment.facade.rest

import com.bosch.pt.csm.cloud.common.blob.model.ImageResolution
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractApiDocumentationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import com.bosch.pt.csm.cloud.projectmanagement.util.doWithAuthorization
import com.bosch.pt.csm.cloud.projectmanagement.util.requestBuilder
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.ResultActions

@ExtendWith(RestDocumentationExtension::class)
@SmartSiteSpringBootTest
@DisplayName("Document attachment api")
class AttachmentApiDocumentationTest : AbstractApiDocumentationTest() {

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
        .submitTaskAttachment()
  }

  @Test
  fun `for getting an attachment`() {
    requestAttachment(getIdentifier("taskAttachment"), ImageResolution.ORIGINAL)
        .andDo(
            MockMvcRestDocumentation.document(
                "get-attachment", ATTACHMENT_PATH_PARAMETERS_DESCRIPTORS))
  }

  private fun requestAttachment(
      attachmentId: UUID,
      size: ImageResolution,
      authorizeAsUser: UserAggregateAvro = fmUser
  ): ResultActions =
      doWithAuthorization(repositories.findUser(authorizeAsUser)) {
        mockMvc.perform(
            requestBuilder(
                get(
                    latestVersionOf("/projects/tasks/activities/attachments/{attachmentId}/{size}"),
                    attachmentId,
                    size),
                objectMapper))
      }

  companion object {

    private val IMAGE_RESOLUTION_VALUES =
        ImageResolution.values().filter { r -> r != ImageResolution.MEDIUM }.joinToString()

    private val ATTACHMENT_PATH_PARAMETERS_DESCRIPTORS =
        pathParameters(
            parameterWithName("attachmentId").description("ID of the attachment to download."),
            parameterWithName("size")
                .description(
                    "Size of attachment to download. Possible values are: $IMAGE_RESOLUTION_VALUES"))
  }
}
