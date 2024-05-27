/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.kafka.eventlistener

import com.bosch.pt.csm.cloud.common.api.AbstractPersistable
import com.bosch.pt.csm.cloud.common.api.LocalEntity
import com.bosch.pt.csm.cloud.common.businesstransaction.jpa.EventOfBusinessTransactionEntity
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.csm.cloud.common.eventstore.AbstractKafkaEvent
import com.bosch.pt.iot.smartsite.application.config.TransactionScopeConfiguration.Companion.NAME
import com.bosch.pt.iot.smartsite.common.kafka.eventstore.EventStore
import com.bosch.pt.iot.smartsite.common.kafka.streamable.AbstractKafkaStreamable
import com.bosch.pt.iot.smartsite.common.model.AbstractReplicatedEntity
import jakarta.annotation.PostConstruct
import java.util.Collections
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.hibernate.Hibernate
import org.hibernate.proxy.HibernateProxy
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Collect all [AbstractKafkaStreamable] entities before they will serialized and saved as event in
 * the database.
 */
@Component
@Scope(value = NAME, proxyMode = TARGET_CLASS)
open class EventCollector(private val eventStore: EventStore) : TransactionSynchronization {

  private val store: MutableMap<Class<*>, MutableMap<UUID, Enum<*>>> = ConcurrentHashMap()

  @PostConstruct
  open fun init() {
    TransactionSynchronizationManager.registerSynchronization(this)
  }

  /**
   * Collect the entity, if it's an instance of [AbstractKafkaStreamable], with its event type in
   * the transaction.
   *
   * @param entity which will be collected
   */
  @Suppress("UNCHECKED_CAST")
  open fun collectKafkaStreamable(entity: Any) {
    var potentialStreamableEntity = entity
    if (isStreamable(potentialStreamableEntity)) {
      // if the entity is a proxy and has to be lazy initialized, we have to unproxy it to publish
      // it correctly to the event stream
      if (potentialStreamableEntity is HibernateProxy) {
        potentialStreamableEntity = Hibernate.unproxy(potentialStreamableEntity)
      }

      val streamableEntity = potentialStreamableEntity as AbstractKafkaStreamable<*, *, Enum<*>>
      requireNotNull(streamableEntity.eventType) { "Event type must not be null!" }

      collect(streamableEntity)
      LOGGER!!.debug(
          "Entity {}#{} is collected in transaction",
          streamableEntity.javaClass.simpleName,
          streamableEntity.id)
    }
  }

  /**
   * Save the entity, if it's an instance of [AbstractKafkaStreamable]. If the event was not
   * previously collected, an [IllegalArgumentException] is thrown.
   *
   * @param entity which will be saved
   */
  @Suppress("UNCHECKED_CAST")
  open fun saveKafkaStreamable(entity: Any) {
    if (isStreamable(entity)) {
      val streamableEntity = entity as AbstractKafkaStreamable<*, *, Enum<*>>

      // get the event type from validator to check whether the entity was already collected in the
      // same transaction
      val eventType =
          getEventType(streamableEntity).orElseThrow {
            IllegalArgumentException(
                "Failed to get EventType for Entity " +
                    streamableEntity.javaClass.simpleName +
                    "[" +
                    streamableEntity.identifier +
                    "]")
          }

      if (eventType != streamableEntity.eventType) {

        if (streamableEntity.eventType == null) {
          streamableEntity.eventType = eventType
        } else {
          throw IllegalArgumentException(
              "Failed to save Entity " +
                  streamableEntity.javaClass.simpleName +
                  "[" +
                  streamableEntity.identifier +
                  "]. EventType is not equal to the previously saved one.")
        }
      }

      eventStore.save(streamableEntity)
      LOGGER!!.debug(
          "Entity {}#{} is saved as event with type {}",
          streamableEntity.javaClass.simpleName,
          streamableEntity.id,
          streamableEntity.eventType)
    }
  }

  /**
   * Checks if the entity ist already collected.
   *
   * @param entity which will be checked if it is already collected
   * @return `true` if the entity is already collected or not streamable, otherwise `false`
   */
  open fun isEntityAlreadyStored(entity: Any): Boolean {
    var potentialStreamableEntity = entity

    if (isStreamable(potentialStreamableEntity)) {
      if (potentialStreamableEntity is HibernateProxy) {
        potentialStreamableEntity = Hibernate.unproxy(potentialStreamableEntity)
      }

      val streamableEntity = potentialStreamableEntity as AbstractKafkaStreamable<*, *, *>

      return if (store[streamableEntity.javaClass] == null) {
        false
      } else store[streamableEntity.javaClass]!!.containsKey(streamableEntity.identifier)
    }

    return true
  }

  private fun collect(event: AbstractKafkaStreamable<*, *, Enum<*>>) {
    require(
        store
            .computeIfAbsent(event.javaClass) { Collections.synchronizedMap(HashMap()) }
            .put(requireNotNull(event.identifier), event.eventType as Enum<*>) == null) {
          "It is not allowed to collect the same aggregate [" +
              event.identifier +
              "|" +
              event.javaClass +
              "] in the same transaction twice."
        }
  }

  /**
   * Removes the entity from the `store` and returns the [ ][AbstractKafkaStreamable.eventType].
   *
   * @param entity to identify the stored entity
   * @return Optional of the stored [AbstractKafkaStreamable.eventType]
   */
  private fun getEventType(entity: AbstractKafkaStreamable<*, *, *>): Optional<Enum<*>> {
    return if (store[entity.javaClass] == null) {
      Optional.empty()
    } else Optional.ofNullable(store[entity.javaClass]!!.remove(entity.identifier))
  }

  private fun isStreamable(entity: Any): Boolean =
      when (entity) {
        is AbstractKafkaStreamable<*, *, *> -> true
        is AbstractKafkaEvent,
        is AbstractReplicatedEntity<*>,
        is LocalEntity<*>,
        is EventOfBusinessTransactionEntity,
        is AbstractSnapshotEntity<*, *> -> {
          LOGGER!!.debug(
              "Entity {}#{} is not streamable",
              entity.javaClass.simpleName,
              (entity as AbstractPersistable<*>?)!!.id)
          false
        }
        else -> {
          throw IllegalStateException(
              "Failed to save Entity " +
                  entity.javaClass.simpleName +
                  ". Entity has an unknown instance and can't be handled.")
        }
      }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(EventCollector::class.java)
  }
}
