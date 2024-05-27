/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.attachment.facade.rest

import com.bosch.pt.csm.cloud.common.util.HttpTestUtils
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Attachment
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.AuditingInformation
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.attachment.facade.rest.response.factory.AttachmentResourceFactory
import java.util.Date
import java.util.UUID
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@SmartSiteSpringBootTest
class AttachmentResourceFactoryTest {

  @Autowired private lateinit var cut: AttachmentResourceFactory

  @BeforeEach
  fun setup() {
    HttpTestUtils.setFakeUrlWithApiVersion(1)
  }

  @Test
  fun verifyAttachmentWithoutDate() {
    val resource =
        cut.build(
            Attachment(
                AuditingInformation(
                    UnresolvedObjectReference("User", UUID.randomUUID(), UUID.randomUUID()),
                    Date(),
                    UnresolvedObjectReference("User", UUID.randomUUID(), UUID.randomUUID()),
                    Date()),
                UUID.randomUUID(),
                null,
                "fileName",
                0L,
                0L,
                0,
                null,
                null,
                null))
    Assertions.assertNull(resource.captureDate)
  }
}
