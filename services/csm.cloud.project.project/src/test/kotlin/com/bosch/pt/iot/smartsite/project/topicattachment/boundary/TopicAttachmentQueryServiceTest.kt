/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topicattachment.boundary

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.iot.smartsite.application.SmartSiteMockKTest
import com.bosch.pt.iot.smartsite.common.i18n.Key.ATTACHMENT_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.attachment.repository.AttachmentRepository
import com.bosch.pt.iot.smartsite.project.topicattachment.repository.TopicAttachmentRepository
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
class TopicAttachmentQueryServiceTest {

  @MockK private lateinit var topicAttachmentRepository: TopicAttachmentRepository

  @Suppress("Unused", "UnusedPrivateMember")
  @MockK
  private lateinit var attachmentRepository: AttachmentRepository

  @InjectMockKs private lateinit var cut: TopicAttachmentQueryService

  @Test
  @DisplayName("Find a topic attachment for a non found identifier fails")
  fun verifyFindOneByIdentifierFailsForNotFoundIdentifier() {

    val identifier = randomUUID()
    every { topicAttachmentRepository.findOneByIdentifier(identifier, AttachmentDto::class.java) }
        .returns(null)

    assertThatExceptionOfType(AggregateNotFoundException::class.java)
        .isThrownBy { cut.findOneByIdentifier(identifier) }
        .withMessageKey(ATTACHMENT_VALIDATION_ERROR_NOT_FOUND)

    verify(exactly = 1) {
      topicAttachmentRepository.findOneByIdentifier(identifier, AttachmentDto::class.java)
    }
  }
}
