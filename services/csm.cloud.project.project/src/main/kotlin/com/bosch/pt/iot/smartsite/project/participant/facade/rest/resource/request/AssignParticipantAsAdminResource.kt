/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.request

import jakarta.validation.constraints.Email

data class AssignParticipantAsAdminResource(@field:Email val email: String)
