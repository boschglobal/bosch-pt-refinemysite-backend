/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.util.HttpTestUtils.setFakeUrlWithApiVersion
import com.bosch.pt.iot.smartsite.application.SmartSiteMockKTest
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.TaskAttachmentResource.Companion.LINK_DELETE
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.factory.TaskAttachmentResourceFactoryHelper
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import java.util.Date
import java.util.UUID.randomUUID
import java.util.function.Supplier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.context.MessageSource
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@SmartSiteMockKTest
@DisplayName("Verifying task attachment resource link ")
class TaskAttachmentResourceTest {

  @Suppress("Unused", "UnusedPrivateMember")
  @MockK
  private lateinit var deletedUserReference: Supplier<ResourceReference>

  @Suppress("Unused", "UnusedPrivateMember")
  @RelaxedMockK
  private lateinit var messageSource: MessageSource

  @Suppress("Unused", "UnusedPrivateMember")
  @SpyK
  private var customLinkBuilderFactory: CustomLinkBuilderFactory = initLinkBuilder()

  @InjectMockKs lateinit var cut: TaskAttachmentResourceFactoryHelper

  @BeforeEach
  fun mockApiVersioning() {
    setFakeUrlWithApiVersion()
  }

  private fun initLinkBuilder(): CustomLinkBuilderFactory {
    val request = MockHttpServletRequest("get", "https://smartsite.de/v1/")
    RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    return CustomLinkBuilderFactory()
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `for delete`(allowedToDelete: Boolean) {

    val resource =
        cut.build(
            AttachmentDto(
                randomUUID(),
                0L,
                "attachment.jpg",
                Date(),
                0L,
                0L,
                0L,
                true,
                false,
                randomUUID(),
                "first",
                "last",
                Date(),
                false,
                randomUUID(),
                "first",
                "last",
                Date(),
                false,
                TaskId()),
            allowedToDelete)

    assertThat(resource.hasLink(LINK_DELETE)).isEqualTo(allowedToDelete)
  }
}
