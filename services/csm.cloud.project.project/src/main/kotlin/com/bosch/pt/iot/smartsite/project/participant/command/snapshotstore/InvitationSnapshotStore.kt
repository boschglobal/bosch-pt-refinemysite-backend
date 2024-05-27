/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJdbc
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.messages.getCreatedByIdentifier
import com.bosch.pt.csm.cloud.common.messages.getLastModifiedByIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.INVITATION
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventEnumAvro.RESENT
import com.bosch.pt.iot.smartsite.common.i18n.Key.INVITATION_VALIDATION_ERROR_INVITATION_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectInvitationContextSnapshotStore
import com.bosch.pt.iot.smartsite.project.participant.InvitationId
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.asInvitationId
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Invitation
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum.FM
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.INVITED
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.InvitationRepository
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import java.sql.Timestamp
import java.util.UUID
import jakarta.persistence.EntityManager
import org.apache.avro.specific.SpecificRecordBase
import org.hibernate.Session
import org.slf4j.Logger
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class InvitationSnapshotStore(
    private val repository: InvitationRepository,
    private val participantRepository: ParticipantRepository,
    private val projectRepository: ProjectRepository,
    namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    entityManager: EntityManager,
    logger: Logger
) :
    AbstractSnapshotStoreJdbc<InvitationEventAvro, InvitationSnapshot, Invitation, InvitationId>(
        namedParameterJdbcTemplate, entityManager, logger),
    ProjectInvitationContextSnapshotStore {

  fun findOrIgnore(identifier: ParticipantId) =
      repository.findOneByParticipantIdentifier(identifier)?.asSnapshot()

  override fun findOrFail(identifier: InvitationId) =
      repository.findOneByIdentifier(identifier)?.asSnapshot()
          ?: throw AggregateNotFoundException(
              INVITATION_VALIDATION_ERROR_INVITATION_NOT_FOUND, identifier.toString())

  override fun findInternal(identifier: UUID) =
      repository.findOneByIdentifier(identifier.asInvitationId())

  // This is an exceptional case since the invitation aggregate was created due to technical
  // reasons (be able to remove the email address from kafka) and is therefore more or less
  // part of the participant. That is also why is it found by participant identifier in some cases
  fun findOrFail(identifier: ParticipantId) =
      repository.findOneByParticipantIdentifier(identifier)?.asSnapshot()
          ?: throw AggregateNotFoundException(
              INVITATION_VALIDATION_ERROR_INVITATION_NOT_FOUND, identifier.toString())

  override fun isDeletedEvent(message: SpecificRecordBase) = false

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase): Boolean =
      key.aggregateIdentifier.type == INVITATION.name &&
          message is InvitationEventAvro &&
          message.name in setOf(CREATED, RESENT)

  override fun handlesTombstoneMessage(key: AggregateEventMessageKey): Boolean =
      key.aggregateIdentifier.type == INVITATION.name

  override fun updateInternal(
      event: InvitationEventAvro,
      currentSnapshot: Invitation?,
      rootContextIdentifier: UUID
  ) {
    when (currentSnapshot == null) {
      true -> {
        createInvitation(event.aggregate)
        setEmailOnParticipant(event.aggregate)
      }
      else -> {
        updateInvitation(event.aggregate)
        removeFromPersistenceContext(currentSnapshot)
      }
    }
  }

  override fun handleTombstoneMessage(key: AggregateEventMessageKey) {
    InvitationId(key.aggregateIdentifier.identifier).let { id ->
      repository.findOneByIdentifier(id)?.let { invitation ->
        // As described above in an edge-case when a user didn't respond to an invitation and the
        // invitation is deleted, the previously created dummy participant has to be deleted again.
        participantRepository.findOneByIdentifier(invitation.participantIdentifier)?.let {
          if (it.status == INVITED) participantRepository.delete(it)
        }
        // Delete the invitation
        repository.deleteByIdentifier(id)
      }
    }
  }

  private fun createInvitation(aggregate: InvitationAggregateAvro) =
      with(aggregate) {
        MapSqlParameterSource()
            .addValue("identifier", getIdentifier().toString())
            .addValue("version", aggregateIdentifier.version)
            .addValue("created_by", auditingInformation.getCreatedByIdentifier().toString())
            .addValue("created_date", Timestamp(auditingInformation.createdDate))
            .addValue(
                "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
            .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
            .addValue("project_identifier", aggregate.projectIdentifier.toString())
            .addValue("participant_identifier", aggregate.participantIdentifier.toString())
            .addValue("email", aggregate.email)
            .addValue("last_sent", Timestamp(aggregate.lastSent))
            .apply { execute(INSERT_INVITATION_STATEMENT, this) }
      }

  private fun updateInvitation(aggregate: InvitationAggregateAvro) =
      with(aggregate) {
        MapSqlParameterSource()
            .addValue("identifier", getIdentifier().toString())
            .addValue("version", aggregateIdentifier.version)
            .addValue(
                "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
            .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
            .addValue("last_sent", Timestamp(aggregate.lastSent))
            .apply { execute(UPDATE_INVITATION_STATEMENT, this) }
      }

  // This can be removed once a read-view has been created to merge information
  // from the invitation and the participant. Currently, the participant is also a
  // read-view that contains the email address from the invitation
  private fun setEmailOnParticipant(aggregate: InvitationAggregateAvro) {
    val participant = findParticipant(aggregate.participantIdentifier.asParticipantId())
    if (participant == null) {
      // Create a dummy participant if the participant cannot be found.
      // This can be the case when the participant was invited but didn't accept the invitation.
      //
      // The dummy participant is deleted again if the participant is still in status INVITED
      // when the tombstone message is processed.
      // When a participant updated event, which changes the participant status from INVITED to
      // VALIDATION, should be restored afterwards, the email address is taken from the user.
      createParticipant(aggregate)
    } else {
      updateParticipant(aggregate)
      entityManager.unwrap(Session::class.java).evict(participant)
    }
  }

  /**
   * This dummy participant that is created here does not have a user or company assigned since they
   * are not known at this point in time. The version "-1" is used to not get a conflict, when the
   * first participant event is received and processed in [ParticipantSnapshotStore].
   */
  private fun createParticipant(aggregate: InvitationAggregateAvro) =
      with(aggregate) {
        val project = findProjectOrFail(projectIdentifier.asProjectId())
        MapSqlParameterSource()
            .addValue("identifier", participantIdentifier.toString())
            .addValue("version", -1)
            .addValue("created_by", auditingInformation.getCreatedByIdentifier().toString())
            .addValue("created_date", Timestamp(auditingInformation.createdDate))
            .addValue(
                "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
            .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
            .addValue("project_id", project.id)
            .addValue("email", email)
            // The role is unknown - take FM as default. It's overwritten with the correct value
            // when the participant UPDATED event is processed later.
            .addValue("role", FM.getPosition())
            .addValue("status", INVITED.getPosition())
            .apply { execute(INSERT_PARTICIPANT_STATEMENT, this) }
      }

  private fun updateParticipant(aggregate: InvitationAggregateAvro) =
      with(aggregate) {
        MapSqlParameterSource()
            .addValue("identifier", participantIdentifier.toString())
            .addValue("email", email)
            .apply { execute(UPDATE_PARTICIPANT_STATEMENT, this) }
      }

  private fun findParticipant(participantIdentifier: ParticipantId): Participant? =
      participantRepository.findOneByIdentifier(participantIdentifier)

  private fun findProjectOrFail(projectIdentifier: ProjectId): Project =
      checkNotNull(projectRepository.findOneByIdentifier(projectIdentifier)) {
        "Project must not be null"
      }

  companion object {

    private const val INSERT_INVITATION_STATEMENT =
        "INSERT INTO invitation " +
            "(identifier, version, created_by, created_date, last_modified_by, last_modified_date, " +
            "project_identifier, participant_identifier, email, last_sent) " +
            "VALUES (:identifier, :version, :created_by, :created_date, :last_modified_by, :last_modified_date, " +
            ":project_identifier, :participant_identifier, :email, :last_sent)"

    private const val UPDATE_INVITATION_STATEMENT =
        "UPDATE invitation " +
            "SET version=:version," +
            "last_modified_by=:last_modified_by," +
            "last_modified_date=:last_modified_date," +
            "last_sent=:last_sent " +
            "WHERE identifier=:identifier AND version=:version-1"

    private const val INSERT_PARTICIPANT_STATEMENT =
        "INSERT INTO project_participant " +
            "(identifier, version, created_by, created_date, last_modified_by, last_modified_date, " +
            "project_id, email, role, status) " +
            "VALUES (:identifier, :version, :created_by, :created_date, :last_modified_by, :last_modified_date, " +
            ":project_id, :email, :role, :status)"

    private const val UPDATE_PARTICIPANT_STATEMENT =
        "UPDATE project_participant SET email=:email WHERE identifier=:identifier"
  }
}
