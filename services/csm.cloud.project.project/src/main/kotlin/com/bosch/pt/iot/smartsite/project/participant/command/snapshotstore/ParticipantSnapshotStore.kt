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
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.getCreatedByIdentifier
import com.bosch.pt.csm.cloud.common.messages.getLastModifiedByIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.CANCELLED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.DEACTIVATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.REACTIVATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.INVITED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.VALIDATION
import com.bosch.pt.iot.smartsite.common.i18n.Key.PARTICIPANT_VALIDATION_ERROR_PARTICIPANT_NOT_FOUND
import com.bosch.pt.iot.smartsite.company.repository.CompanyRepository
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextSnapshotStore
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import java.sql.Timestamp
import java.util.UUID
import jakarta.persistence.EntityManager
import org.apache.avro.specific.SpecificRecordBase
import org.slf4j.Logger
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class ParticipantSnapshotStore(
    private val repository: ParticipantRepository,
    private val cachedRepository: ParticipantSnapshotEntityCache,
    private val companyRepository: CompanyRepository,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    entityManager: EntityManager,
    logger: Logger
) :
    AbstractSnapshotStoreJdbc<
        ParticipantEventG3Avro, ParticipantSnapshot, Participant, ParticipantId>(
        namedParameterJdbcTemplate, entityManager, logger),
    ProjectContextSnapshotStore {

  override fun findOrFail(identifier: ParticipantId) =
      cachedRepository.get(identifier)?.asSnapshot()
          ?: throw AggregateNotFoundException(
              PARTICIPANT_VALIDATION_ERROR_PARTICIPANT_NOT_FOUND, identifier.toString())

  fun findOrIgnore(identifier: ParticipantId) = cachedRepository.get(identifier)?.asSnapshot()

  override fun findInternal(identifier: UUID) = cachedRepository.get(identifier.asParticipantId())

  override fun isDeletedEvent(message: SpecificRecordBase) =
      (message as ParticipantEventG3Avro).name == CANCELLED

  override fun updateInternal(
      event: ParticipantEventG3Avro,
      currentSnapshot: Participant?,
      rootContextIdentifier: UUID
  ) {
    if (event.name == CANCELLED && currentSnapshot != null) {
      deleteParticipant(currentSnapshot)
      cachedRepository.remove(currentSnapshot.identifier)
    } else {
      when (currentSnapshot == null) {
        true -> createParticipant(event.aggregate)
        false -> {
          updateParticipant(event.aggregate, currentSnapshot)
          removeFromPersistenceContext(currentSnapshot)
          cachedRepository.remove(currentSnapshot.identifier)
        }
      }
    }
  }

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase): Boolean =
      key.aggregateIdentifier.type == PARTICIPANT.name &&
          message is ParticipantEventG3Avro &&
          message.name in setOf(CREATED, UPDATED, DEACTIVATED, REACTIVATED, CANCELLED)

  private fun createParticipant(aggregate: ParticipantAggregateG3Avro) {
    with(aggregate) {
      val projectId = findProjectIdOrFail(project)
      val companyId =
          if (status !in setOf(INVITED, VALIDATION)) {
            findCompanyIdOrFail(company)
          } else null
      val user =
          if (status != INVITED) {
            findUserOrFail(user)
          } else null

      MapSqlParameterSource()
          .addValue("identifier", getIdentifier().toString())
          .addValue("version", getVersion())
          .addValue("created_by", auditingInformation.getCreatedByIdentifier().toString())
          .addValue("created_date", Timestamp(auditingInformation.createdDate))
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .addValue("project_id", projectId)
          .addValue("company_id", companyId)
          .addValue("user_id", user?.id)
          .addValue("email", user?.email)
          .addValue("role", ParticipantRoleEnum.valueOf(role.name).getPosition())
          .addValue("status", ParticipantStatusEnum.valueOf(status.name).getPosition())
          .apply { execute(INSERT_STATEMENT, this) }
    }
  }

  private fun updateParticipant(
      aggregate: ParticipantAggregateG3Avro,
      currentSnapshot: Participant
  ) {
    with(aggregate) {
      val companyId =
          if (status !in setOf(INVITED, VALIDATION)) {
            findCompanyIdOrFail(company)
          } else null
      val user =
          if (status != INVITED) {
            findUserOrFail(user)
          } else null

      MapSqlParameterSource()
          .addValue("identifier", getIdentifier().toString())
          .addValue("version", getVersion())
          .addValue(
              "last_modified_by", auditingInformation.getLastModifiedByIdentifier().toString())
          .addValue("last_modified_date", Timestamp(auditingInformation.lastModifiedDate))
          .addValue("company_id", companyId)
          .addValue("user_id", user?.id)
          // In case a dummy participant was created when restoring an invitation before the
          // participant the email has to be used from the current state of the participant
          .addValue("email", user?.email ?: currentSnapshot.email)
          .addValue("role", ParticipantRoleEnum.valueOf(role.name).getPosition())
          .addValue("status", ParticipantStatusEnum.valueOf(status.name).getPosition())
          .apply { execute(UPDATE_STATEMENT, this) }
    }
  }

  private fun deleteParticipant(participant: Participant) = repository.delete(participant)

  private fun findCompanyIdOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): Long =
      requireNotNull(
          companyRepository.findIdByIdentifier(aggregateIdentifierAvro.identifier.toUUID())) {
            "Company must not be null"
          }

  private fun findUserOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): User =
      requireNotNull(
          userRepository.findOneByIdentifier(aggregateIdentifierAvro.identifier.toUUID())) {
            "User must not be null"
          }

  private fun findProjectIdOrFail(aggregateIdentifierAvro: AggregateIdentifierAvro): Long =
      requireNotNull(
          projectRepository.findIdByIdentifier(aggregateIdentifierAvro.identifier.asProjectId())) {
            "Project must not be null"
          }

  companion object {

    private const val INSERT_STATEMENT =
        "INSERT INTO project_participant " +
            "(identifier, version, created_by, created_date, last_modified_by, last_modified_date, " +
            "project_id, company_id, user_id, email, role, status) " +
            "VALUES (:identifier, :version, :created_by, :created_date, :last_modified_by, :last_modified_date, " +
            ":project_id, :company_id, :user_id, :email, :role, :status)"

    private const val UPDATE_STATEMENT =
        "UPDATE project_participant " +
            "SET version=:version," +
            "last_modified_by=:last_modified_by," +
            "last_modified_date=:last_modified_date," +
            "company_id=:company_id," +
            "user_id=:user_id,email=:email," +
            "role=:role," +
            "status=:status " +
            "WHERE identifier=:identifier AND version=:version-1"
  }
}
