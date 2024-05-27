/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.attachment.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractSliceResource
import jakarta.validation.constraints.NotNull

open class AttachmentBatchResource(
    @field:NotNull val attachments: Collection<AttachmentResource>,
    pageNumber: Int,
    pageSize: Int
) : AbstractSliceResource(pageNumber, pageSize)
