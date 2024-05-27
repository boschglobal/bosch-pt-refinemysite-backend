/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot.Companion.INITIAL_SNAPSHOT_VERSION
import com.bosch.pt.iot.smartsite.project.participant.InvitationId
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.command.mapper.InvitationAvroSnapshotMapper
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Invitation
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import java.time.LocalDateTime
import java.time.LocalDateTime.now

data class InvitationSnapshot(
    override val identifier: InvitationId,
    override val version: Long = INITIAL_SNAPSHOT_VERSION,
    override val createdBy: UserId? = null,
    override val createdDate: LocalDateTime? = null,
    override val lastModifiedBy: UserId? = null,
    override val lastModifiedDate: LocalDateTime? = null,
    val projectRef: ProjectId,
    val participantRef: ParticipantId,
    val email: String,
    val lastSent: LocalDateTime = now()
) : VersionedSnapshot, AuditableSnapshot {

  constructor(
      invitation: Invitation
  ) : this(
      invitation.identifier,
      invitation.version,
      invitation.createdBy.get(),
      invitation.createdDate.get(),
      invitation.lastModifiedBy.get(),
      invitation.lastModifiedDate.get(),
      invitation.projectIdentifier,
      invitation.participantIdentifier,
      invitation.email,
      invitation.lastSent)
}

fun Invitation.asSnapshot() = InvitationSnapshot(this)

fun InvitationSnapshot.toCommandHandler() = CommandHandler.of(this, InvitationAvroSnapshotMapper)
