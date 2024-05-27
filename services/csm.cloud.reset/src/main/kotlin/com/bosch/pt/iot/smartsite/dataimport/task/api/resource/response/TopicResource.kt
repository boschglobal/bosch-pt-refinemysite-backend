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
class TopicResource(
    var id: UUID,
    var taskId: UUID,
    var criticality: String? = null,
    var description: String? = null,
    var messages: Int? = null,
    createdBy: ResourceReferenceWithPicture? = null
) : AuditableResource(createdBy = createdBy)
