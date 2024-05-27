/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.messageattachment.boundary

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.iot.smartsite.application.SmartSiteMockKTest
import com.bosch.pt.iot.smartsite.common.i18n.Key.ATTACHMENT_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.messageattachment.repository.MessageAttachmentRepository
import com.bosch.pt.iot.smartsite.util.withMessageKey
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@SmartSiteMockKTest
internal class MessageAttachmentQueryServiceTest {

  @MockK private lateinit var messageAttachmentRepository: MessageAttachmentRepository

  @InjectMockKs private lateinit var cut: MessageAttachmentQueryService

  @Test
  @DisplayName("Find a message attachment for a non found identifier fails")
  fun verifyFindOneByIdentifierFailsForNotFoundIdentifier() {

    val identifier = randomUUID()
    every { messageAttachmentRepository.findOneByIdentifier(identifier, AttachmentDto::class.java) }
        .returns(null)

    assertThatExceptionOfType(AggregateNotFoundException::class.java)
        .isThrownBy { cut.findOneByIdentifier(identifier) }
        .withMessageKey(ATTACHMENT_VALIDATION_ERROR_NOT_FOUND)

    verify(exactly = 1) {
      messageAttachmentRepository.findOneByIdentifier(identifier, AttachmentDto::class.java)
    }
  }
}
