/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.attachment.facade.rest.response

import com.bosch.pt.csm.cloud.projectmanagement.common.facade.rest.resource.AbstractResource
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.rest.resource.ResourceReference
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Date
import java.util.UUID

class AttachmentResource(
    val captureDate: Date? = null,
    val fileName: String,
    val fileSize: Long,
    val imageHeight: Long,
    val imageWidth: Long,
    val topicId: UUID?,
    val messageId: UUID?,
    val taskId: UUID?,
    @JsonProperty("id") val identifier: UUID,
    val createdDate: Date,
    val lastModifiedDate: Date,
    val createdBy: ResourceReference,
    val lastModifiedBy: ResourceReference
) : AbstractResource() {

  companion object {
    const val LINK_PREVIEW = "preview"
    const val LINK_DATA = "data"
    const val LINK_ORIGINAL = "original"
  }
}
