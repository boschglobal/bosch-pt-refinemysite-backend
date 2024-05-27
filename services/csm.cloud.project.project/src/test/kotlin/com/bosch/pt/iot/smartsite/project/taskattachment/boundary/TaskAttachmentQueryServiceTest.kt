/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskattachment.boundary

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.iot.smartsite.application.SmartSiteMockKTest
import com.bosch.pt.iot.smartsite.common.i18n.Key.ATTACHMENT_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.attachment.repository.AttachmentRepository
import com.bosch.pt.iot.smartsite.project.taskattachment.repository.TaskAttachmentRepository
import com.bosch.pt.iot.smartsite.util.withMessageKey
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

@SmartSiteMockKTest
class TaskAttachmentQueryServiceTest {

  @Suppress("Unused", "UnusedPrivateMember")
  @MockK
  lateinit var attachmentRepository: AttachmentRepository

  @MockK lateinit var taskAttachmentRepository: TaskAttachmentRepository

  @InjectMockKs lateinit var cut: TaskAttachmentQueryService

  @Test
  fun `find a task attachment for a non found identifier fails`() {
    val identifier = randomUUID()
    every {
      taskAttachmentRepository.findOneByIdentifier(identifier, AttachmentDto::class.java)
    } returns null

    assertThatExceptionOfType(AggregateNotFoundException::class.java)
        .isThrownBy { cut.findOneByIdentifier(identifier) }
        .withMessageKey(ATTACHMENT_VALIDATION_ERROR_NOT_FOUND)

    verify(exactly = 1) {
      taskAttachmentRepository.findOneByIdentifier(identifier, AttachmentDto::class.java)
    }
  }
}
