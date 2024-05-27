/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.messageattachment.boundary

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.iot.smartsite.application.SmartSiteMockKTest
import com.bosch.pt.iot.smartsite.common.i18n.Key.MESSAGE_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.attachment.boundary.AttachmentService
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.shared.repository.MessageRepository
import com.bosch.pt.iot.smartsite.project.messageattachment.repository.MessageAttachmentRepository
import com.bosch.pt.iot.smartsite.test.RandomData.multiPartFile
import com.bosch.pt.iot.smartsite.util.withMessageKey
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
class MessageAttachmentServiceTest {

  @Suppress("Unused", "UnusedPrivateMember") @MockK private lateinit var idGenerator: IdGenerator

  @MockK private lateinit var messageRepository: MessageRepository

  @Suppress("Unused", "UnusedPrivateMember")
  @MockK
  private lateinit var messageAttachmentRepository: MessageAttachmentRepository

  @Suppress("Unused", "UnusedPrivateMember")
  @MockK
  private lateinit var attachmentService: AttachmentService

  @InjectMockKs private lateinit var cut: MessageAttachmentService

  @Test
  @DisplayName("Save a message attachment for a non found message fails")
  fun verifySaveMessageAttachmentFailsForNotFoundMessage() {

    val messageIdentifier = MessageId()
    val multipartFile: MultipartFile = multiPartFile()

    every { messageRepository.findOneByIdentifier(messageIdentifier) }.returns(null)
    assertThatExceptionOfType(AggregateNotFoundException::class.java)
        .isThrownBy {
          cut.saveMessageAttachment(
              multipartFile.bytes, messageIdentifier, "Test_file", null, TimeZone.getDefault())
        }
        .withMessageKey(MESSAGE_VALIDATION_ERROR_NOT_FOUND)

    verify(exactly = 1) { messageRepository.findOneByIdentifier(messageIdentifier) }
  }
}
