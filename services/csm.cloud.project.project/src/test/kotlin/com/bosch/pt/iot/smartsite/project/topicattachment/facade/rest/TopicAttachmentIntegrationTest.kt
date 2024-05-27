/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicAttachment
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.simulateKafkaListener
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.task.command.service.TaskRequestDeleteService
import com.bosch.pt.iot.smartsite.project.topic.boundary.TopicRequestDeleteService
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.TopicAttachmentController.Companion.DEFAULT_TOPIC_ATTACHMENT_NAME
import com.bosch.pt.iot.smartsite.test.RandomData.multiPartFile
import java.io.IOException
import java.time.ZoneOffset.UTC
import java.util.UUID.randomUUID
import org.apache.commons.lang3.RandomStringUtils.randomAscii
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.OK
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile

@EnableAllKafkaListeners
internal class TopicAttachmentIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired lateinit var cut: TopicAttachmentController

  @Autowired lateinit var taskRequestDeleteService: TaskRequestDeleteService

  @Autowired lateinit var topicRequestDeleteService: TopicRequestDeleteService

  private val topic by lazy { repositories.findTopic(getIdentifier("topic").asTopicId())!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication("userCsm1")
  }

  @Test
  @DisplayName("Check saving a topic attachment sets all properties correctly")
  fun saveSucceedsForAllProperties() {
    val attachmentIdentifier = randomUUID()
    val multipartFile: MultipartFile = multiPartFile()
    val response = cut.save(multipartFile, topic.identifier, attachmentIdentifier, UTC)

    val resource = response.body!!
    assertThat(response.statusCode).isEqualTo(CREATED)
    assertThat(response.body).isNotNull
    assertThat(resource.identifier).isEqualTo(attachmentIdentifier)
    assertThat(resource.fileName).isEqualTo(multipartFile.originalFilename)
    assertThat(resource.fileSize).isEqualTo(multipartFile.size)
    assertThat(resource.taskId).isEqualTo(topic.task.identifier)
    assertThat(resource.topicId).isEqualTo(topic.identifier)
  }

  @Test
  @DisplayName("Check saving a topic attachment sets file name correctly for a blank file name")
  @Throws(IOException::class)
  fun saveSucceedsForBlankFilename() {
    val attachmentIdentifier = randomUUID()
    val multipartFile: MultipartFile = MockMultipartFile(randomAscii(5), multiPartFile().bytes)

    val response = cut.save(multipartFile, topic.identifier, attachmentIdentifier, UTC)
    val resource = response.body!!
    assertThat(response.statusCode).isEqualTo(CREATED)
    assertThat(response.body).isNotNull
    assertThat(resource.fileName).isEqualTo(DEFAULT_TOPIC_ATTACHMENT_NAME)
  }

  @Test
  @DisplayName("Find all topic attachments of a topic succeeds")
  fun findAllSucceeds() {
    eventStreamGenerator
        .submitTopicAttachment("topicAttachment1") { it.attachmentBuilder.fileName = "File1.png" }
        .submitTopicAttachment("topicAttachment2") { it.attachmentBuilder.fileName = "File2.png" }

    val topicIdentifier = getIdentifier("topic").asTopicId()
    val topicAttachment = repositories.findTopicAttachment(getIdentifier("topicAttachment1"))!!
    val topicAttachment2 = repositories.findTopicAttachment(getIdentifier("topicAttachment2"))!!

    val response = cut.findAll(topicIdentifier, false)

    val resources = response.body!!
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(resources).isNotNull
    assertThat(resources.attachments).hasSize(2)
    assertThat(resources.attachments)
        .extracting("fileName", "identifier")
        .containsOnly(
            tuple(topicAttachment.fileName, topicAttachment.identifier),
            tuple(topicAttachment2.fileName, topicAttachment2.identifier))
  }

  @Test
  @DisplayName("Find all topic and message attachments of a topic succeeds")
  fun findAllIncludingChildrenSucceeds() {
    eventStreamGenerator
        .submitTopicAttachment("topicAttachment1") { it.attachmentBuilder.fileName = "File1.png" }
        .submitTopicAttachment("topicAttachment2") { it.attachmentBuilder.fileName = "File2.png" }
        .submitMessageAttachment("messageAttachment1") {
          it.attachmentBuilder.fileName = "File3.png"
        }
        .submitMessageAttachment("messageAttachment2") {
          it.attachmentBuilder.fileName = "File4.png"
        }

    val topicIdentifier = getIdentifier("topic").asTopicId()
    val topicAttachment = repositories.findTopicAttachment(getIdentifier("topicAttachment1"))!!
    val topicAttachment2 = repositories.findTopicAttachment(getIdentifier("topicAttachment2"))!!
    val messageAttachment =
        repositories.findMessageAttachment(getIdentifier("messageAttachment1"))!!
    val messageAttachment2 =
        repositories.findMessageAttachment(getIdentifier("messageAttachment2"))!!

    val response = cut.findAll(topicIdentifier, true)
    val resources = response.body!!
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(resources).isNotNull
    assertThat(resources.attachments).hasSize(4)
    assertThat(resources.attachments)
        .extracting("fileName", "identifier")
        .containsOnly(
            tuple(topicAttachment.fileName, topicAttachment.identifier),
            tuple(topicAttachment2.fileName, topicAttachment2.identifier),
            tuple(messageAttachment.fileName, messageAttachment.identifier),
            tuple(messageAttachment2.fileName, messageAttachment2.identifier))
  }

  @Test
  @DisplayName("No topic attachments are return for a topic of a deleted task")
  fun verifyHidingOfAttachmentsIncludingChildrenOfDeletedTask() {
    eventStreamGenerator.submitTopicAttachment().submitMessageAttachment()

    // Verify initial state
    var response = cut.findAll(topic.identifier, true)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.attachments).hasSize(2)
    response = cut.findAll(topic.identifier, false)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.attachments).hasSize(1)

    // Delete task
    simulateKafkaListener { taskRequestDeleteService.markAsDeleted(topic.task.identifier) }

    // Verify expected state
    response = cut.findAll(topic.identifier, true)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.attachments).isEmpty()
    response = cut.findAll(topic.identifier, false)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.attachments).isEmpty()
  }

  @Test
  @DisplayName("No topic attachments are return of a deleted topic")
  fun verifyHidingOfAttachmentsIncludingChildrenOfDeletedTopic() {
    eventStreamGenerator.submitTopicAttachment().submitMessageAttachment()

    // Verify initial state
    var response = cut.findAll(topic.identifier, true)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.attachments).hasSize(2)
    response = cut.findAll(topic.identifier, false)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.attachments).hasSize(1)

    // Delete topic
    simulateKafkaListener { topicRequestDeleteService.markAsDeleted(topic.identifier) }

    // Verify expected state
    response = cut.findAll(topic.identifier, true)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.attachments).isEmpty()
    response = cut.findAll(topic.identifier, false)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.attachments).isEmpty()
  }
}
