/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.facade.rest.resource.request

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

class CreateBatchRequestResource<T>(
    @field:NotEmpty @field:Size(max = 100) val items: Collection<T>
)
