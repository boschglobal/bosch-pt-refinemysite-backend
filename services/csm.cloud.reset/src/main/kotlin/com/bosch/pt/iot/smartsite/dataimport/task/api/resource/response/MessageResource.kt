/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.task.api.resource.response

import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.AuditableResource
import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response.ResourceReferenceWithPicture
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID

@JsonIgnoreProperties("_embedded", "_links")
class MessageResource(
    var id: UUID,
    var topicId: UUID? = null,
    var content: String? = null,
    createdBy: ResourceReferenceWithPicture? = null
) : AuditableResource(createdBy = createdBy)
