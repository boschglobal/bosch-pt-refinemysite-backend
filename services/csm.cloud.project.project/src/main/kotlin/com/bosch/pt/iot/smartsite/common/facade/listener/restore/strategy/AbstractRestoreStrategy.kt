/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.streamable.restoredb.DetachedEntityUpdateCallback
import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreDbStrategy
import com.bosch.pt.iot.smartsite.common.repository.FindOneByIdentifierRepository
import com.bosch.pt.iot.smartsite.common.util.JpaUtilities
import com.bosch.pt.iot.smartsite.common.util.TimeUtilities
import com.bosch.pt.iot.smartsite.common.util.returnUnit
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import jakarta.persistence.EntityManager
import java.time.LocalDateTime
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.data.domain.Auditable
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

abstract class AbstractRestoreStrategy(
    // Use the entity manager to bypass our entity listeners / event store mechanisms
    protected val entityManager: EntityManager,
    protected val userRepository: UserRepository,
    private val findOneByIdentifierRepository: FindOneByIdentifierRepository
) : RestoreDbStrategy {

  @Transactional(propagation = MANDATORY)
  override fun handle(record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>) {
    if (isNew(record) || isDeleted(record)) {
      doHandle(record)
    }
  }

  fun assertEventNotNull(event: SpecificRecordBase?, key: AggregateEventMessageKey) =
      requireNotNull(event) {
        "Unknown Avro tombstone message received of type: ${key.aggregateIdentifier.type}"
      }

  fun handleInvalidEventType(eventType: String): Unit =
      throw IllegalArgumentException("Invalid event type detected: $eventType")

  fun <T> update(entity: T, updateCallback: DetachedEntityUpdateCallback<T>) {
    // It's required to detach the entity to set versions manually
    entityManager.detach(entity)

    // Run the callback
    updateCallback.update(entity)

    // Use the hibernate session to update the entity without updating the entity version
    JpaUtilities.replicate(entityManager, entity)
  }

  fun findUserOrCreatePlaceholder(aggregateIdentifierAvro: AggregateIdentifierAvro): User {
    val identifier = UUID.fromString(aggregateIdentifierAvro.getIdentifier())
    var user = userRepository.findWithDetailsByIdentifier(identifier)

    if (user == null) {
      user =
          User().apply {
            this.identifier = identifier
            this.version = aggregateIdentifierAvro.getVersion()
            this.anonymize()
          }
      entityManager.persist(user)
    }

    return user
  }

  fun <T> delete(entity: T?) {
    if (entity != null) {
      entityManager.remove(entity)
    }
  }

  fun setAuditAttributes(
      entity: Auditable<User, *, LocalDateTime>,
      auditingInformation: AuditingInformationAvro
  ) =
      entity
          .apply {
            setCreatedBy(findUserOrCreatePlaceholder(auditingInformation.getCreatedBy()))
            setLastModifiedBy(findUserOrCreatePlaceholder(auditingInformation.getLastModifiedBy()))
            setCreatedDate(TimeUtilities.asLocalDateTime(auditingInformation.getCreatedDate()))
            setLastModifiedDate(
                TimeUtilities.asLocalDateTime(auditingInformation.getLastModifiedDate()))
          }
          .returnUnit()

  protected abstract fun doHandle(
      record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
  )

  // TODO: [SMAR-11713] Remove workaround once the event stream is fixed so that the aggregate
  // version is always incremented for a DELETED event (unless it's a tombstone message).
  private fun isDeleted(
      record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
  ): Boolean = record.value()?.get("name").toString() == "DELETED"

  private fun isNew(
      record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
  ): Boolean {
    if (record.value() == null) {
      // Handle tombstone messages as new messages
      return true
    }

    val key = record.key()
    val latest = findOneByIdentifierRepository.findOne(key.aggregateIdentifier.identifier)

    return (latest.isEmpty ||
        requireNotNull(latest.get().version) <= key.aggregateIdentifier.version)
  }
}
