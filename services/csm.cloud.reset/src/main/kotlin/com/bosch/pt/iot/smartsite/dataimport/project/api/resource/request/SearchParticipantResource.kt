/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.dataimport.project.api.resource.request

import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.response.ParticipantStatusEnum
import com.bosch.pt.iot.smartsite.dataimport.project.model.ParticipantRoleEnum
import java.util.UUID

data class SearchParticipantResource(
    val status: Set<ParticipantStatusEnum>? = null,
    val company: UUID? = null,
    val roles: Set<ParticipantRoleEnum>? = null
)
