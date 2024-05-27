/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.shared.repository

import com.bosch.pt.iot.smartsite.project.participant.InvitationId
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Invitation
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import java.time.LocalDateTime
import java.util.Date
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface InvitationRepository :
    JpaRepository<Invitation, Long>, JpaSpecificationExecutor<Invitation> {

  fun findOneByParticipantIdentifier(participantIdentifier: ParticipantId): Invitation?

  fun findOneByProjectIdentifierAndEmail(projectIdentifier: ProjectId, email: String): Invitation?

  fun existsByProjectIdentifierAndEmail(projectIdentifier: ProjectId, email: String): Boolean

  fun findOneByIdentifier(identifier: InvitationId): Invitation?

  fun findAllByEmail(email: String): List<Invitation>

  fun findAllByLastSentBefore(before: LocalDateTime): List<Invitation>

  fun findAllByCreatedDateAfterAndCreatedDateBefore(after: Date, before: Date): List<Invitation>

  fun deleteByIdentifier(identifier: InvitationId)
}
