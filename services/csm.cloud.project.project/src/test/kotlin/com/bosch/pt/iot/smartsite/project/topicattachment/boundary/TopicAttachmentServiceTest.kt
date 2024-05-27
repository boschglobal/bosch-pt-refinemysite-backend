/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topicattachment.boundary

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.iot.smartsite.application.SmartSiteMockKTest
import com.bosch.pt.iot.smartsite.common.i18n.Key.TOPIC_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.attachment.boundary.AttachmentService
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicRepository
import com.bosch.pt.iot.smartsite.project.topicattachment.repository.TopicAttachmentRepository
import com.bosch.pt.iot.smartsite.test.RandomData.multiPartFile
import com.bosch.pt.iot.smartsite.util.withMessageKey
import io.mockk.Called
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import java.util.TimeZone
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.util.IdGenerator
import org.springframework.web.multipart.MultipartFile

@SmartSiteMockKTest
class TopicAttachmentServiceTest {

  @MockK private lateinit var idGenerator: IdGenerator

  @MockK private lateinit var topicRepository: TopicRepository

  @MockK private lateinit var topicAttachmentRepository: TopicAttachmentRepository

  @Suppress("Unused", "UnusedPrivateMember")
  @MockK
  private lateinit var attachmentService: AttachmentService

  @InjectMockKs private lateinit var cut: TopicAttachmentService

  @Test
  @DisplayName("Save a topic attachment for a non found topic fails")
  fun verifySaveMessageAttachmentFailsNotFoundTopic() {

    val topicIdentifier = TopicId()
    val multipartFile: MultipartFile = multiPartFile()
    every { topicRepository.findOneByIdentifier(topicIdentifier) }.returns(null)

    assertThatExceptionOfType(AggregateNotFoundException::class.java)
        .isThrownBy {
          cut.saveTopicAttachment(
              multipartFile.bytes, topicIdentifier, "Test_file", null, TimeZone.getDefault())
        }
        .withMessageKey(TOPIC_VALIDATION_ERROR_NOT_FOUND)

    verify(exactly = 1) { topicRepository.findOneByIdentifier(topicIdentifier) }
    verify { listOf(topicAttachmentRepository, idGenerator) wasNot Called }
  }
}
