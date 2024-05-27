/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum

class RfvResource(val key: DayCardReasonEnum, val active: Boolean, val name: String) :
    AbstractResource() {

  companion object {
    const val LINK_RFV_ACTIVATE = "activate"
    const val LINK_RFV_DEACTIVATE = "deactivate"
    const val LINK_RFV_UPDATE = "update"
    const val LINK_UPDATE_RFV = "updateRfv"
  }
}
