/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.simulateKafkaListener
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.message.domain.asMessageId
import com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.MessageAttachmentController.Companion.DEFAULT_MESSAGE_ATTACHMENT_NAME
import com.bosch.pt.iot.smartsite.project.task.command.service.TaskRequestDeleteService
import com.bosch.pt.iot.smartsite.project.topic.boundary.TopicRequestDeleteService
import com.bosch.pt.iot.smartsite.test.RandomData.multiPartFile
import java.io.IOException
import java.time.ZoneOffset.UTC
import java.util.UUID.randomUUID
import org.apache.commons.lang3.RandomStringUtils.randomAscii
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.OK
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile

@EnableAllKafkaListeners
class MessageAttachmentIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired lateinit var cut: MessageAttachmentController

  @Autowired lateinit var taskRequestDeleteService: TaskRequestDeleteService

  @Autowired lateinit var topicRequestDeleteService: TopicRequestDeleteService

  private val message by lazy { repositories.findMessage(getIdentifier("message").asMessageId())!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication("userCsm1")
  }

  @Test
  @DisplayName("Check saving a message attachment sets all properties correctly")
  fun saveSucceedsForAllProperties() {
    val attachmentIdentifier = randomUUID()
    val multipartFile: MultipartFile = multiPartFile()
    val response = cut.save(multipartFile, message.identifier, attachmentIdentifier, UTC)
    val resource = response.body!!

    assertThat(response.statusCode).isEqualTo(CREATED)
    assertThat(response.body).isNotNull
    assertThat(resource.identifier).isEqualTo(attachmentIdentifier)
    assertThat(resource.fileName).isEqualTo(multipartFile.originalFilename)
    assertThat(resource.fileSize).isEqualTo(multipartFile.size)
    assertThat(resource.taskId).isEqualTo(message.topic.task.identifier)
    assertThat(resource.topicId).isEqualTo(message.topic.identifier)
    assertThat(resource.messageId).isEqualTo(message.identifier)
  }

  @Test
  @DisplayName("Check saving a message attachment sets file name correctly for a blank file name")
  @Throws(IOException::class)
  fun saveSucceedsForBlankFilename() {
    val attachmentIdentifier = randomUUID()
    val multipartFile = MockMultipartFile(randomAscii(5), multiPartFile().bytes)
    val response = cut.save(multipartFile, message.identifier, attachmentIdentifier, UTC)

    val resource = response.body!!
    assertThat(response.statusCode).isEqualTo(CREATED)
    assertThat(response.body).isNotNull
    assertThat(resource.fileName).isEqualTo(DEFAULT_MESSAGE_ATTACHMENT_NAME)
  }

  @Test
  @DisplayName("Find all message attachments of a message succeeds")
  fun findAllSucceeds() {
    eventStreamGenerator
        .submitMessageAttachment("messageAttachment1") {
          it.attachmentBuilder.fileName = "File1.png"
        }
        .submitMessageAttachment("messageAttachment2") {
          it.attachmentBuilder.fileName = "File2.png"
        }

    val messageIdentifier = getIdentifier("message").asMessageId()
    val messageAttachment =
        repositories.findMessageAttachment(getIdentifier("messageAttachment1"))!!
    val messageAttachment2 =
        repositories.findMessageAttachment(getIdentifier("messageAttachment2"))!!

    val response = cut.findAll(messageIdentifier)

    val resources = response.body!!
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(resources).isNotNull
    assertThat(resources.attachments).hasSize(2)
    assertThat(resources.attachments)
        .extracting("fileName", "identifier")
        .containsOnly(
            Assertions.tuple(messageAttachment.fileName, messageAttachment.identifier),
            Assertions.tuple(messageAttachment2.fileName, messageAttachment2.identifier))
  }

  @Test
  @DisplayName("No message attachments are return for a message of a deleted task")
  fun verifyHidingOfAttachmentsOfDeletedTask() {
    eventStreamGenerator.submitMessageAttachment()

    // Verify initial state
    var response = cut.findAll(message.identifier)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.attachments).hasSize(1)

    // Delete task
    simulateKafkaListener { taskRequestDeleteService.markAsDeleted(message.topic.task.identifier) }

    // Verify expected state
    response = cut.findAll(message.identifier)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.attachments).isEmpty()
  }

  @Test
  @DisplayName("No message attachments are return for a message of a deleted topic")
  fun verifyHidingOfAttachmentsOfDeletedTopic() {
    eventStreamGenerator.submitMessageAttachment()

    // Verify initial state
    var response = cut.findAll(message.identifier)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.attachments).hasSize(1)

    // Delete topic
    simulateKafkaListener { topicRequestDeleteService.markAsDeleted(message.topic.identifier) }

    // Verify expected state
    response = cut.findAll(message.identifier)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.attachments).isEmpty()
  }
}
