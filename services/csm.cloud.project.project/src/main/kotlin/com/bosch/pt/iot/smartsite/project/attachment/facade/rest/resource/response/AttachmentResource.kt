/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.attachment.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.response.AbstractAuditableResource
import java.util.Date
import java.util.Objects
import java.util.UUID

open class AttachmentResource
@JvmOverloads
constructor(
    identifier: UUID,
    version: Long,
    createdDate: Date,
    lastModifiedDate: Date,
    createdBy: ResourceReference,
    lastModifiedBy: ResourceReference,
    val captureDate: Date? = null,
    val fileName: String,
    val fileSize: Long,
    val imageHeight: Long? = null,
    val imageWidth: Long? = null
) :
    AbstractAuditableResource(
        identifier, version, createdDate, createdBy, lastModifiedDate, lastModifiedBy) {

  @ExcludeFromCodeCoverage
  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is AttachmentResource) {
      return false
    }
    if (!super.equals(other)) {
      return false
    }
    return (fileSize == other.fileSize &&
        captureDate == other.captureDate &&
        fileName == other.fileName &&
        imageHeight == other.imageHeight &&
        imageWidth == other.imageWidth)
  }

  @ExcludeFromCodeCoverage
  override fun hashCode(): Int =
      Objects.hash(super.hashCode(), captureDate, fileName, fileSize, imageHeight, imageWidth)
}
