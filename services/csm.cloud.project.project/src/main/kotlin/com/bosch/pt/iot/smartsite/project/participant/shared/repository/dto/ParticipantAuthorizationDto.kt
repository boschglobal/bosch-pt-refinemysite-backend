/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.participant.shared.repository.dto

import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import java.util.UUID

data class ParticipantAuthorizationDto(
    val identifier: ParticipantId,
    val projectIdentifier: ProjectId,
    val userIdentifier: UUID,
    val companyIdentifier: UUID,
    val role: ParticipantRoleEnum
)
