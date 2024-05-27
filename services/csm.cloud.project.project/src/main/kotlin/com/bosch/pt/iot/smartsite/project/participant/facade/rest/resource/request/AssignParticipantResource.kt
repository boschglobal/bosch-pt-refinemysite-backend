/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.request

import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import jakarta.validation.constraints.Email

data class AssignParticipantResource(@field:Email val email: String, val role: ParticipantRoleEnum)
