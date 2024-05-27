/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReferenceWithPicture
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.UUID

data class NotificationResource(
    @JsonProperty("id") val identifier: UUID,
    val read: Boolean = false,
    val actor: ResourceReferenceWithPicture,
    val date: Instant,
    val summary: NotificationSummaryDto,
    val changes: String? = null,
    val context: NotificationContextDto,
    @JsonProperty("object") val objectReference: ObjectReference
) : AbstractResource() {
  companion object {
    const val LINK_READ = "read"
  }
}

data class NotificationContextDto(val project: ResourceReference, val task: ResourceReference)

data class NotificationSummaryDto(
    val template: String,
    val values: Map<String, PlaceholderValueDto>
)

data class PlaceholderValueDto(val type: String, val id: UUID, val text: String)

data class ObjectReference(val type: String, val identifier: UUID)
