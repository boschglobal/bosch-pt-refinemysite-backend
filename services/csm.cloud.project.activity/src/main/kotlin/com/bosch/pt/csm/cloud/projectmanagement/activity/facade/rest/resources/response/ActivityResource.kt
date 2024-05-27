/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.resources.response

import com.bosch.pt.csm.cloud.projectmanagement.common.facade.rest.resource.AbstractResource
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.rest.resource.ResourceReferenceWithPicture
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Date
import java.util.UUID

data class ActivityResource(
    @JsonProperty("id") val identifier: UUID,
    val user: ResourceReferenceWithPicture,
    val date: Date,
    @JsonProperty("description") val summary: SummaryDto,
    @JsonProperty("changes") val details: List<String>
) : AbstractResource()

data class SummaryDto(
    val template: String,
    @JsonProperty("values") val references: Map<String, ObjectReferenceDto>
)

data class ObjectReferenceDto(val type: String, val id: UUID, val text: String)
