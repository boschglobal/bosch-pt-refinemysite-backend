/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.NamedEnumReference
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

data class DayCardResource(
    override val id: UUID,
    override val version: Long,
    override val createdDate: Date,
    override val lastModifiedDate: Date,
    override val createdBy: ResourceReference,
    override val lastModifiedBy: ResourceReference,
    val task: ResourceReference,
    val title: String,
    val manpower: BigDecimal,
    val notes: String?,
    val status: DayCardStatusEnum,
    val reason: NamedEnumReference<DayCardReasonEnum>?
) :
    AbstractAuditableResource(
        id, version, createdDate, createdBy, lastModifiedDate, lastModifiedBy) {

  companion object {
    const val LINK_UPDATE_DAYCARD = "update"
    const val LINK_DELETE_DAYCARD = "delete"
    const val LINK_CANCEL_DAYCARD = "cancel"
    const val LINK_COMPLETE_DAYCARD = "complete"
    const val LINK_APPROVE_DAYCARD = "approve"
    const val LINK_RESET_DAYCARD = "reset"
  }
}
