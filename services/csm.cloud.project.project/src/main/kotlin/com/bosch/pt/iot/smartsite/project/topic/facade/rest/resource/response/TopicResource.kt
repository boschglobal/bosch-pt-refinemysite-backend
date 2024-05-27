/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReferenceWithPicture
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import java.net.URI
import java.util.Date
import java.util.UUID

class TopicResource(
    id: UUID,
    version: Long,
    createdDate: Date,
    createdBy: ResourceReference,
    lastModifiedDate: Date,
    lastModifiedBy: ResourceReference,
    val taskId: TaskId,
    val criticality: TopicCriticalityEnum,
    val description: String?,
    val messages: Long,
    creatorPicture: URI?
) :
    AbstractAuditableResource(
        id = id,
        version = version,
        createdDate = createdDate,
        createdBy = getCreatorReference(createdBy, creatorPicture),
        lastModifiedDate = lastModifiedDate,
        lastModifiedBy = lastModifiedBy) {

  companion object {
    const val LINK_ESCALATE = "escalate"
    const val LINK_DEESCALATE = "deescalate"
    const val LINK_DELETE = "delete"
    const val LINK_MESSAGE = "messages"
    const val LINK_CREATE_MESSAGE = "createMessage"
    const val EMBEDDED_TOPIC_ATTACHMENTS = "attachments"

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
