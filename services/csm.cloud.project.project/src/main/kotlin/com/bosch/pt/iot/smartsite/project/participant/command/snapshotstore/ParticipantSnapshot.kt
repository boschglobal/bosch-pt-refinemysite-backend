/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AuditableSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot.Companion.INITIAL_SNAPSHOT_VERSION
import com.bosch.pt.iot.smartsite.company.api.CompanyId
import com.bosch.pt.iot.smartsite.company.api.asCompanyId
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.command.mapper.ParticipantAvroSnapshotMapper
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import java.time.LocalDateTime

data class ParticipantSnapshot(
    override val identifier: ParticipantId,
    override val version: Long = INITIAL_SNAPSHOT_VERSION,
    override val createdBy: UserId? = null,
    override val createdDate: LocalDateTime? = null,
    override val lastModifiedBy: UserId? = null,
    override val lastModifiedDate: LocalDateTime? = null,
    val projectRef: ProjectId,
    val companyRef: CompanyId? = null,
    val userRef: UserId? = null,
    val role: ParticipantRoleEnum,
    val status: ParticipantStatusEnum
) : VersionedSnapshot, AuditableSnapshot {

  constructor(
      participant: Participant
  ) : this(
      participant.identifier,
      participant.version,
      participant.createdBy.get(),
      participant.createdDate.get(),
      participant.lastModifiedBy.get(),
      participant.lastModifiedDate.get(),
      participant.project!!.identifier,
      participant.company?.identifier?.asCompanyId(),
      participant.user?.identifier?.asUserId(),
      participant.role!!,
      participant.status!!)
}

fun Participant.asSnapshot() = ParticipantSnapshot(this)

fun ParticipantSnapshot.toCommandHandler() = CommandHandler.of(this, ParticipantAvroSnapshotMapper)
