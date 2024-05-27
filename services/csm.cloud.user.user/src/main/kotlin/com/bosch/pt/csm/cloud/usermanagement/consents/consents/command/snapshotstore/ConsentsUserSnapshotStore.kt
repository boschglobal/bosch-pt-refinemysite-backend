/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.consents.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJpa
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot.Companion.INITIAL_SNAPSHOT_VERSION
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.shared.model.ConsentsUser
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.shared.model.UserConsent
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.shared.repository.ConsentsUserRepository
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersionId
import com.bosch.pt.csm.cloud.usermanagement.consents.eventstore.ConsentsContextSnapshotStore
import com.bosch.pt.csm.cloud.usermanagement.consents.messages.ConsentDelayedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.consents.messages.UserConsentedEventAvro
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class ConsentsUserSnapshotStore(private val repository: ConsentsUserRepository) :
    AbstractSnapshotStoreJpa<SpecificRecordBase, ConsentsUserSnapshot, ConsentsUser, UserId>(),
    ConsentsContextSnapshotStore {
  override fun findInternal(identifier: UUID) = repository.findByIdentifier(UserId(identifier))

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase) =
      message is ConsentDelayedEventAvro || message is UserConsentedEventAvro

  override fun isDeletedEvent(message: SpecificRecordBase) = false

  override fun updateInternal(event: SpecificRecordBase, currentSnapshot: ConsentsUser?): Long {
    when (event) {
      is ConsentDelayedEventAvro -> {
        val updatedSnapshot =
            currentSnapshot
                ?: ConsentsUser(0L.toLocalDateTimeByMillis(), mutableListOf()).apply {
                  identifier = UserId(event.aggregateIdentifier.identifier)
                  version = event.aggregateIdentifier.version
                  setCreatedBy(UserId(event.auditingInformation.user.toUUID()))
                  setCreatedDate(event.auditingInformation.date.toLocalDateTimeByMillis())
                  setLastModifiedBy(UserId(event.auditingInformation.user.toUUID()))
                  setLastModifiedDate(event.auditingInformation.date.toLocalDateTimeByMillis())
                }

        updatedSnapshot.apply {
          delayedAt = event.auditingInformation.date.toLocalDateTimeByMillis()

          version = event.aggregateIdentifier.version
          setLastModifiedBy(UserId(event.auditingInformation.user.toUUID()))
          setLastModifiedDate(event.auditingInformation.date.toLocalDateTimeByMillis())
        }

        return repository.saveAndFlush(updatedSnapshot).version
      }
      is UserConsentedEventAvro -> {
        val updatedSnapshot =
            currentSnapshot
                ?: ConsentsUser(0L.toLocalDateTimeByMillis(), mutableListOf()).apply {
                  identifier = UserId(event.aggregateIdentifier.identifier)
                  version = event.aggregateIdentifier.version
                  setCreatedBy(UserId(event.auditingInformation.user.toUUID()))
                  setCreatedDate(event.auditingInformation.date.toLocalDateTimeByMillis())
                  setLastModifiedBy(UserId(event.auditingInformation.user.toUUID()))
                  setLastModifiedDate(event.auditingInformation.date.toLocalDateTimeByMillis())
                }

        updatedSnapshot.apply {
          consents.add(
              UserConsent(
                  event.consentedAt.toLocalDateByMillis(),
                  DocumentVersionId(event.documentVersionIdentifier)
              ))

          version = event.aggregateIdentifier.version
          setLastModifiedBy(UserId(event.auditingInformation.user.toUUID()))
          setLastModifiedDate(event.auditingInformation.date.toLocalDateTimeByMillis())
        }

        return repository.saveAndFlush(updatedSnapshot).version
      }
      else -> error("Snapshot can't handle event ${event::class}. This should not happen.")
    }
  }

  override fun findOrFail(identifier: UserId): ConsentsUserSnapshot {
    throw NotImplementedError("not needed")
  }

  fun find(identifier: UserId): ConsentsUserSnapshot? =
      repository.findByIdentifier(identifier)?.asValueObject()

  fun findOrCreateSnapshot(identifier: UserId) =
      find(identifier)
          ?: ConsentsUserSnapshot(
              0L.toLocalDateTimeByMillis(), emptyList(), identifier, INITIAL_SNAPSHOT_VERSION)
}
