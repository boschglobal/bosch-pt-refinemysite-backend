/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.attachment.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource
import java.util.Objects
import org.springframework.hateoas.Link
import org.springframework.hateoas.RepresentationModel

open class AttachmentListResource : RepresentationModel<AbstractResource> {

  val attachments: Collection<AttachmentResource>

  constructor(attachments: Collection<AttachmentResource>) {
    this.attachments = attachments
  }

  constructor(initialLink: Link, attachments: Collection<AttachmentResource>) : super(initialLink) {
    this.attachments = attachments
  }

  constructor(
      initialLinks: Iterable<Link>,
      attachments: Collection<AttachmentResource>
  ) : super(initialLinks) {
    this.attachments = attachments
  }

  @ExcludeFromCodeCoverage
  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is AttachmentListResource) {
      return false
    }
    if (!super.equals(other)) {
      return false
    }
    return attachments == other.attachments
  }

  @ExcludeFromCodeCoverage
  override fun hashCode(): Int = Objects.hash(super.hashCode(), attachments)
}
