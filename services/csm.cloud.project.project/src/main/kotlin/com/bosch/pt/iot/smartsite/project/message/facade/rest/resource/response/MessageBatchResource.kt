/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractSliceResource

class MessageBatchResource(
    val messages: Collection<MessageResource>,
    pageNumber: Int,
    pageSize: Int
) : AbstractSliceResource(pageNumber, pageSize)
