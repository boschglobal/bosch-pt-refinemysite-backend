/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.request

import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.rfv.boundary.dto.UpdateRfvDto
import com.bosch.pt.iot.smartsite.project.rfv.model.RfvCustomization.Companion.MAX_NAME_LENGTH
import jakarta.validation.constraints.Size

data class UpdateRfvResource(

    // reason
    val key: DayCardReasonEnum,

    // active
    val active: Boolean,

    // optional name
    @field:Size(min = 1, max = MAX_NAME_LENGTH) val name: String? = null
) {

  fun toDto(projectIdentifier: ProjectId) = UpdateRfvDto(projectIdentifier, key, active, name)
}
