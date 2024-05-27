/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.repository.dto

import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.dto.ParticipantAuthorizationDto
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import java.util.UUID

data class RelationAuthorizationDto(
    // Don't reorder these attributes! It is instantiated by a Hibernate query.
    val projectIdentifier: ProjectId,
    val relationIdentifier: UUID,
    val createdByParticipantIdentifier: ParticipantId,
    val createdByCompanyIdentifier: UUID
) {

  fun createdBy(participant: ParticipantAuthorizationDto) =
      createdByParticipantIdentifier == participant.identifier

  fun createdByCompanyOf(participant: ParticipantAuthorizationDto) =
      createdByCompanyIdentifier == participant.companyIdentifier
}
