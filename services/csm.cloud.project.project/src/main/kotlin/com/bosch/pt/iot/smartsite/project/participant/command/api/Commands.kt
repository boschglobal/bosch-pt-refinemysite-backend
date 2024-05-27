/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.api

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.iot.smartsite.company.api.CompanyId
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId

@Suppress("UnnecessaryAbstractClass")
abstract class ParticipantCommand(open val identifier: ParticipantId)

data class AssignFirstParticipantCommand(
    val projectRef: ProjectId,
    val companyRef: CompanyId,
    val userRef: UserId
)

data class AssignActiveParticipantCommand(
    val identifier: ParticipantId = ParticipantId(),
    val projectRef: ProjectId,
    val companyRef: CompanyId,
    val userRef: UserId,
    val role: ParticipantRoleEnum
)

data class AcceptInvitationCommand(override val identifier: ParticipantId) :
    ParticipantCommand(identifier)

data class ActivateParticipantCommand(
    override val identifier: ParticipantId,
    val companyRef: CompanyId
) : ParticipantCommand(identifier)

data class AssignParticipantCommand(
    // This is optional for now to know whether it has been filled from the URL or not
    // In case the target is an inactive participant, and the assign endpoint is used
    // without a participant identifier, generating a random new one causes a problem.
    val identifier: ParticipantId?,
    val projectRef: ProjectId,
    val email: String,
    val role: ParticipantRoleEnum
)

data class AssignParticipantAsAdminCommand(
    val projectRef: ProjectId,
    val email: String,
)

data class CancelInvitationCommand(override val identifier: ParticipantId) :
    ParticipantCommand(identifier)

data class DeactivateParticipantCommand(override val identifier: ParticipantId) :
    ParticipantCommand(identifier)

data class RemoveParticipantCommand(override val identifier: ParticipantId) :
    ParticipantCommand(identifier)

data class ResendInvitationCommand(override val identifier: ParticipantId) :
    ParticipantCommand(identifier)

data class UpdateParticipantCommand(
    override val identifier: ParticipantId,
    val version: Long,
    val role: ParticipantRoleEnum
) : ParticipantCommand(identifier)
