/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.request

import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum
import java.util.UUID

data class SearchParticipantResource(
    val status: Set<ParticipantStatusEnum>?,
    val company: UUID?,
    val roles: Set<ParticipantRoleEnum>?
)
