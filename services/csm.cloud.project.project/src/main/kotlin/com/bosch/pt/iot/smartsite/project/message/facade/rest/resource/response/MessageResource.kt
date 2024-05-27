/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReferenceWithPicture
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import java.net.URI
import java.util.Date
import java.util.UUID

class MessageResource(
    id: UUID,
    version: Long,
    createdDate: Date,
    lastModifiedDate: Date,
    createdBy: ResourceReference,
    lastModifiedBy: ResourceReference,
    val topicId: TopicId,
    val content: String?,
    creatorPicture: URI
) :
    AbstractAuditableResource(
        id = id,
        version = version,
        createdDate = createdDate,
        createdBy = getCreatorReference(createdBy, creatorPicture),
        lastModifiedDate = lastModifiedDate,
        lastModifiedBy = lastModifiedBy) {

  companion object {
    const val EMBEDDED_MESSAGE_ATTACHMENTS = "attachments"
    const val LINK_DELETE = "delete"

    private fun getCreatorReference(
        createdBy: ResourceReference,
        creatorPicture: URI?
    ): ResourceReference =
        if (creatorPicture == null) {
          ResourceReference(createdBy.identifier, createdBy.displayName)
        } else {
          ResourceReferenceWithPicture(createdBy.identifier, createdBy.displayName, creatorPicture)
        }
  }
}
