/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.simulateKafkaListener
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.exceptions.BatchIdentifierTypeNotSupportedException
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.DAYCARD
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.TASK
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED
import com.bosch.pt.iot.smartsite.common.repository.PageableDefaults.DEFAULT_PAGE_REQUEST
import com.bosch.pt.iot.smartsite.project.task.command.service.TaskRequestDeleteService
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.TaskAttachmentController.Companion.DEFAULT_TASK_ATTACHMENT_NAME
import com.bosch.pt.iot.smartsite.project.topic.boundary.TopicRequestDeleteService
import com.bosch.pt.iot.smartsite.test.RandomData.multiPartFile
import io.mockk.mockk
import java.time.ZoneOffset.UTC
import java.util.UUID.randomUUID
import org.apache.commons.lang3.RandomStringUtils.randomAscii
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile

@EnableAllKafkaListeners
class TaskAttachmentIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired lateinit var cut: TaskAttachmentController

  @Autowired lateinit var taskRequestDeleteService: TaskRequestDeleteService

  @Autowired lateinit var topicRequestDeleteService: TopicRequestDeleteService

  private val taskIdentifier by lazy { getIdentifier("task").asTaskId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication("userCsm2")
  }

  @Test
  fun `check saving a task attachment sets all properties correctly`() {
    val attachmentIdentifier = randomUUID()
    val multipartFile: MultipartFile = multiPartFile()

    val response = cut.save(multipartFile, taskIdentifier, attachmentIdentifier, UTC)

    val resource = response.body!!
    assertThat(response.statusCode).isEqualTo(CREATED)
    assertThat(response.body).isNotNull
    assertThat(resource.identifier).isEqualTo(attachmentIdentifier)
    assertThat(resource.fileName).isEqualTo(multipartFile.originalFilename)
    assertThat(resource.fileSize).isEqualTo(multipartFile.size)
    assertThat(resource.taskId).isEqualTo(taskIdentifier)
  }

  @Test
  @Throws(Exception::class)
  fun `check saving a task attachment sets file name correctly for a blank file name`() {
    val attachmentIdentifier = randomUUID()
    val multipartFile = MockMultipartFile(randomAscii(5), multiPartFile().bytes)

    val response = cut.save(multipartFile, taskIdentifier, attachmentIdentifier, UTC)

    val resource = response.body!!
    assertThat(response.statusCode).isEqualTo(CREATED)
    assertThat(response.body).isNotNull
    assertThat(resource.fileName).isEqualTo(DEFAULT_TASK_ATTACHMENT_NAME)
  }

  @Test
  fun `Find all task attachments of a task succeeds`() {
    eventStreamGenerator
        .submitTaskAttachment("taskAttachment1") { it.attachmentBuilder.fileName = "File1.png" }
        .submitTaskAttachment("taskAttachment2") { it.attachmentBuilder.fileName = "File2.png" }
        .submitTopicAttachment(randomString()) { it.attachmentBuilder.fileName = "File3.png" }
        .submitTopicAttachment(randomString()) { it.attachmentBuilder.fileName = "File4.png" }
        .submitMessageAttachment(randomString()) { it.attachmentBuilder.fileName = "File5.png" }
        .submitMessageAttachment(randomString()) { it.attachmentBuilder.fileName = "File6.png" }

    val taskWithAttachments = repositories.findTaskWithDetails(getIdentifier("task").asTaskId())!!
    val taskAttachment = repositories.findTaskAttachment(getIdentifier("taskAttachment1"))!!
    val taskAttachment2 = repositories.findTaskAttachment(getIdentifier("taskAttachment2"))!!

    val response = cut.findAll(taskWithAttachments.identifier, false)

    val resources = response.body!!
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(resources).isNotNull
    assertThat(resources.attachments).hasSize(2)
    assertThat(resources.attachments)
        .extracting("fileName", "identifier")
        .containsOnly(
            tuple(taskAttachment.fileName, taskAttachment.identifier),
            tuple(taskAttachment2.fileName, taskAttachment2.identifier))
  }

  @Test
  fun `find all task, topic and message attachments of a task succeeds`() {
    eventStreamGenerator
        .submitTaskAttachment("taskAttachment1") { it.attachmentBuilder.fileName = "File1.png" }
        .submitTaskAttachment("taskAttachment2") { it.attachmentBuilder.fileName = "File2.png" }
        .submitTopicAttachment("topicAttachment1") { it.attachmentBuilder.fileName = "File3.png" }
        .submitTopicAttachment("topicAttachment2") { it.attachmentBuilder.fileName = "File4.png" }
        .submitMessageAttachment("messageAttachment1") {
          it.attachmentBuilder.fileName = "File5.png"
        }
        .submitMessageAttachment("messageAttachment2") {
          it.attachmentBuilder.fileName = "File6.png"
        }

    val taskWithAttachments = repositories.findTaskWithDetails(getIdentifier("task").asTaskId())!!
    val taskAttachment = repositories.findTaskAttachment(getIdentifier("taskAttachment1"))!!
    val taskAttachment2 = repositories.findTaskAttachment(getIdentifier("taskAttachment2"))!!
    val topicAttachment = repositories.findTopicAttachment(getIdentifier("topicAttachment1"))!!
    val topicAttachment2 = repositories.findTopicAttachment(getIdentifier("topicAttachment2"))!!
    val messageAttachment =
        repositories.findMessageAttachment(getIdentifier("messageAttachment1"))!!
    val messageAttachment2 =
        repositories.findMessageAttachment(getIdentifier("messageAttachment2"))!!

    val response = cut.findAll(taskWithAttachments.identifier, true)

    val resources = response.body!!
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(resources).isNotNull
    assertThat(resources.attachments).hasSize(6)
    assertThat(resources.attachments)
        .extracting("fileName", "identifier")
        .containsOnly(
            tuple(taskAttachment.fileName, taskAttachment.identifier),
            tuple(taskAttachment2.fileName, taskAttachment2.identifier),
            tuple(topicAttachment.fileName, topicAttachment.identifier),
            tuple(topicAttachment2.fileName, topicAttachment2.identifier),
            tuple(messageAttachment.fileName, messageAttachment.identifier),
            tuple(messageAttachment2.fileName, messageAttachment2.identifier))
  }

  @Nested
  @DisplayName("Find all task, topic and message attachments of a set of tasks")
  inner class FindAllByTaskIdentifiers {

    @Test
    fun succeeds() {
      eventStreamGenerator
          .submitTask("task1")
          .submitTopicG2(randomString())
          .submitMessage(randomString())
          .submitTaskAttachment("taskAttachment1") { it.attachmentBuilder.fileName = "File1.png" }
          .submitTopicAttachment("topicAttachment1") { it.attachmentBuilder.fileName = "File2.png" }
          .submitMessageAttachment("messageAttachment1") {
            it.attachmentBuilder.fileName = "File3.png"
          }
          .submitTask("task2")
          .submitTopicG2(randomString())
          .submitMessage(randomString())
          .submitTaskAttachment("taskAttachment2") { it.attachmentBuilder.fileName = "File4.png" }
          .submitTopicAttachment("topicAttachment2") { it.attachmentBuilder.fileName = "File5.png" }
          .submitMessageAttachment("messageAttachment2") {
            it.attachmentBuilder.fileName = "File6.png"
          }

      val task1Identifier = getIdentifier("task1")
      val task2Identifier = getIdentifier("task2")

      val taskAttachment1 = repositories.findTaskAttachment(getIdentifier("taskAttachment1"))!!
      val taskAttachment2 = repositories.findTaskAttachment(getIdentifier("taskAttachment2"))!!
      val topicAttachment1 = repositories.findTopicAttachment(getIdentifier("topicAttachment1"))!!
      val topicAttachment2 = repositories.findTopicAttachment(getIdentifier("topicAttachment2"))!!
      val messageAttachment1 =
          repositories.findMessageAttachment(getIdentifier("messageAttachment1"))!!
      val messageAttachment2 =
          repositories.findMessageAttachment(getIdentifier("messageAttachment2"))!!

      val resource = BatchRequestResource(setOf(task1Identifier, task2Identifier))

      val response = cut.findAllByTaskIdentifiers(resource, DEFAULT_PAGE_REQUEST, TASK)

      val resources = response.body!!
      assertThat(response.statusCode).isEqualTo(OK)
      assertThat(resources).isNotNull
      assertThat(resources.attachments).hasSize(6)
      assertThat(resources.attachments)
          .extracting("fileName", "identifier")
          .containsOnly(
              tuple(taskAttachment1.fileName, taskAttachment1.identifier),
              tuple(topicAttachment1.fileName, topicAttachment1.identifier),
              tuple(messageAttachment1.fileName, messageAttachment1.identifier),
              tuple(taskAttachment2.fileName, taskAttachment2.identifier),
              tuple(topicAttachment2.fileName, topicAttachment2.identifier),
              tuple(messageAttachment2.fileName, messageAttachment2.identifier))
    }

    @Test
    fun `fails due to unsupported identifier type`() {
      assertThat(
              catchThrowableOfType(
                      {
                        cut.findAllByTaskIdentifiers(
                            BatchRequestResource(emptySet()), mockk(), DAYCARD)
                      },
                      BatchIdentifierTypeNotSupportedException::class.java)
                  .messageKey)
          .isEqualTo(COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED)
    }
  }

  @Test
  fun `delete task attachments succeeds`() {
    eventStreamGenerator.submitTaskAttachment()
    val taskAttachmentIdentifier = getIdentifier("taskAttachment")

    val deleteResponse = cut.delete(taskAttachmentIdentifier)
    val findAllResponse = cut.findAll(taskIdentifier, true)

    val resources = findAllResponse.body!!
    assertThat(deleteResponse.statusCode).isEqualTo(NO_CONTENT)
    assertThat(findAllResponse.statusCode).isEqualTo(OK)
    assertThat(resources).isNotNull
    assertThat(resources.attachments).isEmpty()
  }

  @Test
  fun `no task attachments are return for a deleted task`() {
    eventStreamGenerator.submitTaskAttachment().submitTopicAttachment().submitMessageAttachment()
    val taskAttachment = repositories.findTaskAttachment(getIdentifier("taskAttachment"))!!

    // Verify initial state
    var response = cut.findAll(taskIdentifier, true)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.attachments).hasSize(3)
    response = cut.findAll(taskIdentifier, false)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.attachments).hasSize(1)

    // Delete task
    simulateKafkaListener {
      taskRequestDeleteService.markAsDeleted(taskAttachment.task!!.identifier)
    }

    // Verify expected state
    response = cut.findAll(taskIdentifier, true)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.attachments).isEmpty()
    response = cut.findAll(taskIdentifier, false)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.attachments).isEmpty()
  }

  @Test
  fun `no topic attachments are return for a deleted topic of a task`() {
    eventStreamGenerator.submitTaskAttachment().submitTopicAttachment().submitMessageAttachment()

    val taskAttachment = repositories.findTaskAttachment(getIdentifier("taskAttachment"))!!
    val topicAttachment = repositories.findTopicAttachment(getIdentifier("topicAttachment"))!!

    // Verify initial state
    var response = cut.findAll(taskAttachment.task!!.identifier, true)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.attachments).hasSize(3)
    response = cut.findAll(taskAttachment.task!!.identifier, false)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.attachments).hasSize(1)

    // Delete topic
    simulateKafkaListener {
      topicRequestDeleteService.markAsDeleted(topicAttachment.topic!!.identifier)
    }

    // Verify expected state
    response = cut.findAll(taskAttachment.task!!.identifier, true)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.attachments).hasSize(1)
    response = cut.findAll(taskAttachment.task!!.identifier, false)
    assertThat(response.statusCode).isEqualTo(OK)
    assertThat(response.body!!.attachments).hasSize(1)
  }
}
