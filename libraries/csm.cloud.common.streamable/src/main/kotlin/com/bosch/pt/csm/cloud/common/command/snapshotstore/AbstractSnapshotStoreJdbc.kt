/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.csm.cloud.common.command.snapshotstore.EventVersionValidator.canApply
import com.bosch.pt.csm.cloud.common.eventstore.EventSourceEnum
import com.bosch.pt.csm.cloud.common.eventstore.EventSourceEnum.ONLINE
import com.bosch.pt.csm.cloud.common.eventstore.EventSourceEnum.RESTORE
import com.bosch.pt.csm.cloud.common.eventstore.SnapshotStore
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import jakarta.persistence.EntityManager
import java.util.UUID
import org.apache.avro.generic.GenericRecord
import org.apache.avro.specific.SpecificRecordBase
import org.slf4j.Logger
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

/**
 * This class implements common logic of the interface methods from [SnapshotStore]. It also defines
 * a set of abstract methods that a subclass need to implement. It assumes that updates of the
 * snapshot store are done using the namedParameterJdbcTemplate and version information is handled
 * manually.
 *
 * @param userRepository: The userRepository to be used in
 * @param E: Type of the event class that is handled by the snapshot store
 * @param SE: Type of the external representation of the snapshot
 * @param SI: Type of the internal (persisted) representation of the snapshot (JPA Entity)
 */
abstract class AbstractSnapshotStoreJdbc<
    E : SpecificRecordBase,
    SE : VersionedSnapshot,
    SI : AbstractSnapshotEntity<*, *>,
    EID : UuidIdentifiable>(
    protected val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    protected val entityManager: EntityManager,
    protected val logger: Logger
) : SnapshotStore {

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
   * store).If the check passes, it calls [AbstractSnapshotStoreV3.updateInternal] where the actual
   * logic to update the internal representation of the current snapshot based on the received
   * message need to be implemented.
   */
  @Suppress("NestedBlockDepth", "UNCHECKED_CAST")
  override fun handleMessage(
      key: AggregateEventMessageKey,
      message: SpecificRecordBase,
      source: EventSourceEnum
  ) {
    (message as E).let { event ->
      findInternal(event.getIdentifier()).apply {
        if (!isDuplicateDeletedEvent(this != null, message, source) &&
            canApply(this?.version, event.getAggregateIdentifier().version, source)) {
          updateInternal(message, this, key.rootContextIdentifier)
        } else {
          logger.info("Skipping update of snapshot store for current event")
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
   */
  protected abstract fun updateInternal(event: E, currentSnapshot: SI?, rootContextIdentifier: UUID)

  /**
   * This method needs to be implemented by the subclass to return the internal representation of
   * the current snapshot of the aggregate identified by the given input parameter
   */
  protected abstract fun findInternal(identifier: UUID): SI?

  /**
   * This method executes a SQL statement containing placeholders and populates the given
   * parameters. Furthermore, it asserts that a single database row was affected by the statement
   */
  protected fun execute(sql: String, parameter: MapSqlParameterSource) {
    namedParameterJdbcTemplate.update(sql, parameter).apply {
      check(this == 1) { "Saving the snapshot failed" }
    }
  }

  /**
   * This method removes the snapshot from the persistence context. This is required when an update
   * statement has been sent to the database using JDBC which leaves the persistence context in an
   * inconsistent state.
   */
  protected fun removeFromPersistenceContext(snapshot: SI) {
    entityManager.detach(snapshot)
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
}
