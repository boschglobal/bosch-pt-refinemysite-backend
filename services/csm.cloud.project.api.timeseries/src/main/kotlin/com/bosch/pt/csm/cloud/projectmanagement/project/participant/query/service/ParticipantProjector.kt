/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.service

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.company.company.domain.asCompanyId
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.domain.asParticipantId
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.Participant
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.ParticipantMapper
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.ParticipantRoleEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.ParticipantStatusEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.ParticipantVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.repository.ParticipantRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class ParticipantProjector(private val repository: ParticipantRepository) {

  fun onParticipantEvent(aggregate: ParticipantAggregateG3Avro) {
    val existingParticipant =
        repository.findOneByIdentifier(aggregate.getIdentifier().asParticipantId())

    if (existingParticipant == null || aggregate.getVersion() > existingParticipant.version) {
      (existingParticipant?.updateFromParticipantAggregate(aggregate)
              ?: aggregate.toNewProjection())
          .apply { repository.save(this) }
    }
  }

  private fun ParticipantAggregateG3Avro.toNewProjection(): Participant {
    val participantVersion = this.newParticipantVersion()

    return ParticipantMapper.INSTANCE.fromParticipantVersion(
        participantVersion = participantVersion,
        identifier = aggregateIdentifier.identifier.toUUID().asParticipantId(),
        history = listOf(participantVersion))
  }

  private fun Participant.updateFromParticipantAggregate(
      aggregate: ParticipantAggregateG3Avro
  ): Participant {
    val participantVersion = aggregate.newParticipantVersion()

    return ParticipantMapper.INSTANCE.fromParticipantVersion(
        participantVersion = participantVersion,
        identifier = this.identifier,
        history = this.history.toMutableList().also { it.add(participantVersion) })
  }

  private fun ParticipantAggregateG3Avro.newParticipantVersion(): ParticipantVersion {
    val isNew = this.aggregateIdentifier.version == 0L
    val auditUser: UserId
    val auditDate: LocalDateTime
    if (isNew) {
      auditUser = UserId(this.auditingInformation.createdBy.identifier.toUUID())
      auditDate = this.auditingInformation.createdDate.toLocalDateTimeByMillis()
    } else {
      auditUser = UserId(this.auditingInformation.lastModifiedBy.identifier.toUUID())
      auditDate = this.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis()
    }

    return ParticipantVersion(
        version = this.aggregateIdentifier.version,
        project = this.project.identifier.toUUID().asProjectId(),
        company = this.company.identifier.toUUID().asCompanyId(),
        user = this.user.identifier.toUUID().asUserId(),
        role = ParticipantRoleEnum.valueOf(this.role.name),
        status = ParticipantStatusEnum.valueOf(this.status.name),
        eventAuthor = auditUser,
        eventDate = auditDate)
  }
}
