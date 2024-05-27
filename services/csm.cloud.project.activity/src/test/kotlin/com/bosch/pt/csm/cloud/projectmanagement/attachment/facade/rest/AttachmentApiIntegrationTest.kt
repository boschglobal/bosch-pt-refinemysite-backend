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
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractActivityIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.exceptions.ResourceNotFoundException
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.rest.DeletedAttachmentUriBuilder.FILE_NAME_DELETED_IMAGE_LARGE
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.rest.DeletedAttachmentUriBuilder.FILE_NAME_DELETED_IMAGE_SMALL
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro.DRAFT
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.util.doWithAuthorization
import com.bosch.pt.csm.cloud.projectmanagement.util.requestBuilder
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import io.mockk.every
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.test.web.servlet.ResultActions

@SmartSiteSpringBootTest
@DisplayName("Verify attachment api")
class AttachmentApiIntegrationTest : AbstractActivityIntegrationTest() {

  @BeforeEach
  fun init() {
    every { blobStoreService.generateSignedUrlForImage(any(), any(), any(), any()) } throws
        ResourceNotFoundException("not found")

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
        .submitTopicG2()
        .submitMessage()
        .submitTaskAttachment()
        .submitTopicAttachment()
        .submitMessageAttachment()
        .submitMessage(eventType = MessageEventEnumAvro.DELETED)
        .submitTopicG2(eventType = TopicEventEnumAvro.DELETED)
        .submitTaskAttachment(eventType = TaskAttachmentEventEnumAvro.DELETED)
  }

  @ValueSource(strings = ["ORIGINAL", "MEDIUM", "FULL"])
  @ParameterizedTest
  fun `returns link large placeholder image if a task attachment was deleted`(resolution: String) {
    val response =
        requestAttachment(getIdentifier("taskAttachment"), ImageResolution.valueOf(resolution))
            .andReturn()
            .response
    assertThat(response.getHeader(LOCATION))
        .isEqualTo("http://localhost/$FILE_NAME_DELETED_IMAGE_LARGE")
  }

  @Test
  fun `returns link to small placeholder image if a task attachment was deleted`() {
    val response =
        requestAttachment(getIdentifier("taskAttachment"), ImageResolution.SMALL)
            .andReturn()
            .response
    assertThat(response.getHeader(LOCATION))
        .isEqualTo("http://localhost/$FILE_NAME_DELETED_IMAGE_SMALL")
  }

  @ValueSource(strings = ["ORIGINAL", "MEDIUM", "FULL"])
  @ParameterizedTest
  fun `returns link large placeholder image if a topic attachment was deleted`(resolution: String) {
    val response =
        requestAttachment(getIdentifier("topicAttachment"), ImageResolution.valueOf(resolution))
            .andReturn()
            .response
    assertThat(response.getHeader(LOCATION))
        .isEqualTo("http://localhost/$FILE_NAME_DELETED_IMAGE_LARGE")
  }

  @Test
  fun `returns link to small placeholder image if a topic attachment was deleted`() {
    val response =
        requestAttachment(getIdentifier("topicAttachment"), ImageResolution.SMALL)
            .andReturn()
            .response
    assertThat(response.getHeader(LOCATION))
        .isEqualTo("http://localhost/$FILE_NAME_DELETED_IMAGE_SMALL")
  }

  @ValueSource(strings = ["ORIGINAL", "MEDIUM", "FULL"])
  @ParameterizedTest
  fun `returns link large placeholder image if a message attachment was deleted`(
      resolution: String
  ) {
    val response =
        requestAttachment(getIdentifier("messageAttachment"), ImageResolution.valueOf(resolution))
            .andReturn()
            .response
    assertThat(response.getHeader(LOCATION))
        .isEqualTo("http://localhost/$FILE_NAME_DELETED_IMAGE_LARGE")
  }

  @Test
  fun `returns link to small placeholder image if a message attachment was deleted`() {
    val response =
        requestAttachment(getIdentifier("messageAttachment"), ImageResolution.SMALL)
            .andReturn()
            .response
    assertThat(response.getHeader(LOCATION))
        .isEqualTo("http://localhost/$FILE_NAME_DELETED_IMAGE_SMALL")
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
}
