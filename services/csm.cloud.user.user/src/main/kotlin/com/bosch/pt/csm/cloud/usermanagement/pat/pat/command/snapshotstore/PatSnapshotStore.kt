/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJpa
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.EventAuditingInformationAvro
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.COMMON_VALIDATION_ERROR_DATA_INTEGRITY_VIOLATED
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.PAT_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.csm.cloud.usermanagement.pat.eventstore.PatSnapshotStore
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatCreatedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatScopeEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatTypeEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatUpdatedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.PatId
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.asPatId
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.Pat
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.PatScopeEnum
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.PatTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.repository.PatRepository
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
@Suppress("TooManyFunctions")
class PatSnapshotStore(private val repository: PatRepository) :
    AbstractSnapshotStoreJpa<SpecificRecordBase, PatSnapshot, Pat, PatId>(), PatSnapshotStore {
  override fun findInternal(identifier: UUID): Pat? =
      repository.findByIdentifier(identifier.asPatId())

  // no deleted events since we use tombstones
  override fun isDeletedEvent(message: SpecificRecordBase): Boolean = false

  override fun updateInternal(event: SpecificRecordBase, currentSnapshot: Pat?): Long =
      when (event) {
        is PatCreatedEventAvro -> createSnapshot(currentSnapshot, event)
        is PatUpdatedEventAvro -> updateSnapshot(requireNotNull(currentSnapshot), event)
        // delete is handled as tombstone
        else -> error("Snapshot can't handle event ${event::class}. This should not happen.")
      }

  private fun createSnapshot(
      currentSnapshot: Pat?,
      event: PatCreatedEventAvro,
  ): Long {
    require(currentSnapshot == null) {
      throw DataIntegrityViolationException(COMMON_VALIDATION_ERROR_DATA_INTEGRITY_VIOLATED)
    }
    val createdSnapshot =
        Pat().apply {
          identifier = PatId(event.aggregateIdentifier.identifier)
          version = event.aggregateIdentifier.version

          impersonatedUser = event.impersonatedUser.identifier.asUserId()
          description = event.description
          type = event.type.toPatTypeEnum()
          scopes = event.scopes.toPatScopeEnumList()
          hash = event.hash
          issuedAt = event.issuedAt.toLocalDateTimeByMillis()
          expiresAt = event.expiresAt.toLocalDateTimeByMillis()

          setCreatedBy(UserId(event.auditingInformation.user.toUUID()))
          setCreatedDate(event.auditingInformation.date.toLocalDateTimeByMillis())
          this.applyLastModified(event.auditingInformation)
        }
    return repository.saveAndFlush(createdSnapshot).version
  }

  private fun updateSnapshot(currentSnapshot: Pat, event: PatUpdatedEventAvro): Long =
      repository
          .saveAndFlush(
              currentSnapshot.apply {
                this.description = event.description
                this.expiresAt = event.expiresAt.toLocalDateTimeByMillis()
                this.scopes = event.scopes.toPatScopeEnumList()

                this.applyLastModified(event.auditingInformation)
              })
          .version

  private fun Pat.applyLastModified(auditingInformation: EventAuditingInformationAvro) {
    this.setLastModifiedBy(auditingInformation.user.toUUID().asUserId())
    this.setLastModifiedDate(auditingInformation.date.toLocalDateTimeByMillis())
  }

  override fun findOrFail(identifier: PatId): PatSnapshot =
      repository.findByIdentifier(identifier)?.asSnapshot()
          ?: throw AggregateNotFoundException(PAT_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase) =
      message is PatCreatedEventAvro || message is PatUpdatedEventAvro

  override fun handlesTombstoneMessage(key: AggregateEventMessageKey) = true

  override fun handleTombstoneMessage(key: AggregateEventMessageKey) =
      PatId(key.aggregateIdentifier.identifier).let { repository.deleteByIdentifier(it) }

  private fun MutableList<PatScopeEnumAvro>.toPatScopeEnumList() =
      this.map { PatScopeEnum.valueOf(it.name) }.toMutableList()

  private fun PatTypeEnumAvro.toPatTypeEnum() = PatTypeEnum.valueOf(this.name)
}
