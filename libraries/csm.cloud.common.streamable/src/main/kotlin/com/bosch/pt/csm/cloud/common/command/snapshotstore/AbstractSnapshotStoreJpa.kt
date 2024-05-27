/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.EventVersionValidator.canApply
import com.bosch.pt.csm.cloud.common.eventstore.EventSourceEnum
import com.bosch.pt.csm.cloud.common.eventstore.EventSourceEnum.ONLINE
import com.bosch.pt.csm.cloud.common.eventstore.EventSourceEnum.RESTORE
import com.bosch.pt.csm.cloud.common.eventstore.SnapshotStore
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.common.messages.getCreatedByIdentifier
import com.bosch.pt.csm.cloud.common.messages.getLastModifiedByIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.apache.avro.generic.GenericRecord
import org.apache.avro.specific.SpecificRecordBase
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This class implements common logic of the interface methods from [SnapshotStore]. It also defines
 * a set of abstract methods that a subclass need to implement. It is assumed, that the snapshot is
 * created and updated using JPA / Hibernate and that version information is handled by Hibernate.
 *
 * @param userRepository: The userRepository to be used in
 * @param E: Type of the event class that is handled by the snapshot store
 * @param SE: Type of the external representation of the snapshot
 * @param SI: Type of the internal (persisted) representation of the snapshot (JPA Entity)
 */
abstract class AbstractSnapshotStoreJpa<
    E : SpecificRecordBase,
    SE : VersionedSnapshot,
    SI : AbstractSnapshotEntity<*, *>,
    EID : UuidIdentifiable> : SnapshotStore {

  /**
   * This method returns the current snapshot of an aggregate identified by the given input
   * parameter. The method should throw an exception in case no snapshot is found. It should be used
   * in command handlers to load the current state of an aggregate when command targets an existing
   * aggregate instance.
   *
   * @return external representation of the snapshot that hides any internal persistence information
   */
  abstract fun findOrFail(identifier: EID): SE

  /**
   * This method implements important checks regarding aggregate versions to avoid repeating this in
   * every snapshot store implementation. It makes sure the current snapshot of the aggregate that
   * the received message belongs to is loaded (if it already exists) and compares the version of
   * the aggregate's snapshot with the aggregate version received with the message. To do this, it
   * calls [EventVersionValidator.canApply]. The evaluation logic makes sure, that no message is
   * applied twice or that no older message is applied to the current snapshot. This is used to
   * determine concurrent access to an aggregate when receiving messages from a command handler or
   * to determine duplicate messages received from the event stream (when restoring the snapshot
   * store).If the check passes, it calls [AbstractSnapshotStoreJpa.updateInternal] where the actual
   * logic to update the internal representation of the current snapshot based on the received
   * message need to be implemented. Upon return, a check is made if the new version of the
   * aggregate snapshot equals the one of the received message. This way, consistency between the
   * version of the aggregate snapshot and the version of the aggregate in the messages is
   * guaranteed.
   */
  @Suppress("UNCHECKED_CAST")
  override fun handleMessage(
      key: AggregateEventMessageKey,
      message: SpecificRecordBase,
      source: EventSourceEnum
  ) {
    (message as E).let { event ->
      findInternal(event.getIdentifier()).apply {
        if (!isDuplicateDeletedEvent(this != null, message, source) &&
            canApply(this?.version, event.getAggregateIdentifier().version, source)) {
          updateInternal(message, this).also {
            assertSnapshotVersionEqualsAggregateVersionInEvent(
                event.getAggregateIdentifier().version, it)
          }
        } else {
          LOGGER.info("Skipping update of snapshot store for current event")
        }
      }
    }
  }

  /** Decides, if a received message is a duplicated delete event. */
  private fun isDuplicateDeletedEvent(
      snapshotExists: Boolean,
      message: SpecificRecordBase,
      source: EventSourceEnum
  ) =
      if (isDeletedEvent(message) && !snapshotExists) {
        when (source) {
          RESTORE -> true
          ONLINE ->
              error(
                  "An aggregate delete event was sent to the event bus although no snapshot of the aggregate exists")
        }
      } else {
        false
      }

  /** Decides, if a message is an event saying that the aggregate was deleted. */
  protected abstract fun isDeletedEvent(message: SpecificRecordBase): Boolean

  /**
   * This method needs to be implemented by the subclass to apply the changes recorded in the event
   * to the internal representation (JPA entity) of the current snapshot.
   *
   * @return the version of the snapshot after applying the event
   */
  protected abstract fun updateInternal(event: E, currentSnapshot: SI?): Long

  /**
   * This method needs to be implemented by the subclass to return the internal representation of
   * the current snapshot of the aggregate identified by the given input parameter
   */
  protected abstract fun findInternal(identifier: UUID): SI?

  private fun assertSnapshotVersionEqualsAggregateVersionInEvent(
      eventVersion: Long,
      snapshotVersion: Long
  ) {
    if (eventVersion != snapshotVersion) error("Snapshot version does not equal event version")
  }

  /**
   * Convenient method to set audit attributes of an [AbstractSnapshotEntity] based on auditing
   * information from the event
   */
  protected fun <T : AbstractSnapshotEntity<Long, *>> setAuditAttributes(
      entity: T,
      auditingInformation: AuditingInformationAvro
  ) =
      entity.apply {
        setCreatedBy(auditingInformation.getCreatedByIdentifier().asUserId())
        setLastModifiedBy(auditingInformation.getLastModifiedByIdentifier().asUserId())
        setAuditDateAttributes(this, auditingInformation)
      }

  /**
   * Convenient method to just set the audit dates of an [AbstractSnapshotEntity] based on auditing
   * information from the event. This might be required for self-referencing entities.
   */
  protected fun <T : AbstractSnapshotEntity<Long, *>> setAuditDateAttributes(
      entity: T,
      auditingInformation: AuditingInformationAvro
  ) =
      entity.apply {
        setCreatedDate(asLocalDateTime(auditingInformation.createdDate))
        setLastModifiedDate(asLocalDateTime(auditingInformation.lastModifiedDate))
      }

  // The following private extension functions are used to get common attributes from avro messages
  // that have no specific super class and therefore only be retrieved by using methods of
  // SpecificRecordBase

  private fun SpecificRecordBase.getIdentifier(): UUID =
      getAggregateIdentifier().identifier.toUUID()

  private fun SpecificRecordBase.getAggregateIdentifier(): AggregateIdentifierAvro =
      if (hasField("aggregate")) {
        getAvroProperty("aggregate.aggregateIdentifier")
      } else {
        getAvroProperty("aggregateIdentifier")
      }

  private fun <T> SpecificRecordBase.getAvroProperty(propertyPath: String): T {
    val properties = propertyPath.split(".").toTypedArray()
    var currentNode: Any? = this

    for (property in properties) {
      currentNode = (currentNode as GenericRecord)[property]
    }

    @Suppress("UNCHECKED_CAST") return currentNode as T
  }

  companion object {
    @JvmStatic
    protected val LOGGER: Logger = LoggerFactory.getLogger(AbstractSnapshotStoreJpa::class.java)

    fun asLocalDateTime(date: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(date), ZoneOffset.UTC)
  }
}
