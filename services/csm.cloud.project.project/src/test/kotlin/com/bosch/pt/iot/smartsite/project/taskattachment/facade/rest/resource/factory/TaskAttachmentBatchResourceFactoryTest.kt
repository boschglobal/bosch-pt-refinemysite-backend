/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.common.util.HttpTestUtils.setFakeUrlWithApiVersion
import com.bosch.pt.iot.smartsite.application.SmartSiteMockKTest
import com.bosch.pt.iot.smartsite.project.attachment.facade.rest.resource.response.factory.AttachmentResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.task.authorization.TaskAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.factory.TaskAttachmentBatchResourceFactory
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.MessageSource
import org.springframework.data.domain.SliceImpl

@SmartSiteMockKTest
@DisplayName("Verifying task attachment batch resource factory ")
class TaskAttachmentBatchResourceFactoryTest {

  @Suppress("Unused", "UnusedPrivateMember")
  @MockK
  private lateinit var messageSource: MessageSource

  @Suppress("Unused", "UnusedPrivateMember")
  @MockK
  private lateinit var taskAuthorizationComponent: TaskAuthorizationComponent

  @Suppress("Unused", "UnusedPrivateMember")
  @MockK
  private lateinit var helper: AttachmentResourceFactoryHelper

  @InjectMockKs lateinit var cut: TaskAttachmentBatchResourceFactory

  @BeforeEach
  fun mockApiVersioning() {
    setFakeUrlWithApiVersion()
  }

  @Test
  fun `return a correct resource for a empty slice`() {
    val attachments = SliceImpl(emptyList<AttachmentDto>())
    val resource = cut.build(attachments)
    assertThat(resource).isNotNull
    assertThat(resource.attachments).isEmpty()
    assertThat(resource.pageNumber).isEqualTo(0)
    assertThat(resource.pageSize).isEqualTo(0)
  }
}
