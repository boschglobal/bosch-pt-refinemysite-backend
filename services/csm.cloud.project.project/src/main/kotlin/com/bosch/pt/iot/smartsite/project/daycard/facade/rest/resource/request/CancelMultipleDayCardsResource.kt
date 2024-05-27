/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.request

import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.VersionedIdentifier
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.VersionedUpdateBatchRequestResource
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum

data class CancelMultipleDayCardsResource(
    override val items: Collection<VersionedIdentifier>,
    val reason: DayCardReasonEnum
) : VersionedUpdateBatchRequestResource(items)
