/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskattachment.boundary

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.iot.smartsite.application.SmartSiteMockKTest
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.attachment.boundary.AttachmentService
import com.bosch.pt.iot.smartsite.project.attachment.model.AttachmentImageResolution
import com.bosch.pt.iot.smartsite.project.attachment.repository.AttachmentRepository
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.taskattachment.repository.TaskAttachmentRepository
import com.bosch.pt.iot.smartsite.test.RandomData.multiPartFile
import com.bosch.pt.iot.smartsite.util.withMessageKey
import io.mockk.Called
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import java.net.URL
import java.util.TimeZone
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.util.IdGenerator
import org.springframework.web.multipart.MultipartFile

@SmartSiteMockKTest
class TaskAttachmentServiceTest {

  @MockK lateinit var idGenerator: IdGenerator

  @MockK lateinit var taskRepository: TaskRepository

  @Suppress("Unused", "UnusedPrivateMember")
  @MockK
  lateinit var attachmentRepository: AttachmentRepository

  @MockK lateinit var taskAttachmentRepository: TaskAttachmentRepository

  @MockK lateinit var attachmentService: AttachmentService

  @InjectMockKs lateinit var cut: TaskAttachmentService

  @ParameterizedTest
  @EnumSource(value = AttachmentImageResolution::class)
  fun `generate blob url calls the attachment boundary service for`(
      imageResolution: AttachmentImageResolution
  ) {
    val attachmentIdentifier = randomUUID()
    every { attachmentService.generateBlobAccessUrl(attachmentIdentifier, imageResolution) } returns
        URL("https://blobstore.azure.com")

    cut.generateBlobAccessUrl(attachmentIdentifier, imageResolution)
    verify(exactly = 1) {
      attachmentService.generateBlobAccessUrl(attachmentIdentifier, imageResolution)
    }
  }

  @Test
  fun `save a task attachment for a non found task fails`() {
    val taskIdentifier = TaskId()
    val multipartFile: MultipartFile = multiPartFile()
    every { taskRepository.findOneByIdentifier(taskIdentifier) } returns null

    assertThatExceptionOfType(AggregateNotFoundException::class.java)
        .isThrownBy {
          cut.saveTaskAttachment(
              multipartFile.bytes, taskIdentifier, "Test_file", null, TimeZone.getDefault())
        }
        .withMessageKey(TASK_VALIDATION_ERROR_NOT_FOUND)

    verify(exactly = 1) { taskRepository.findOneByIdentifier(taskIdentifier) }
    verify { listOf(idGenerator, taskAttachmentRepository) wasNot Called }
  }
}
