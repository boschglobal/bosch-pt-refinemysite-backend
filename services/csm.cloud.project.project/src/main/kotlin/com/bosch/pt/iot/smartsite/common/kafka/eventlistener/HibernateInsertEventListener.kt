/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.kafka.eventlistener

import org.hibernate.HibernateException
import org.hibernate.event.spi.PersistContext
import org.hibernate.event.spi.PersistEvent
import org.hibernate.event.spi.PersistEventListener
import org.hibernate.event.spi.PreInsertEvent
import org.hibernate.event.spi.PreInsertEventListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * This class supports the process of persisting an entity. <br></br> The [.onPersist] or
 * [.onPersist] is called before the [.onPreInsert].
 *
 * * [PersistEvent] is fired after [jakarta.persistence.EntityManager.persist] and the entity will be
 * **collected** in the transaction
 * * [PreInsertEvent] is fired before the real SQL `INSERT` statement and the entity will be
 * **serialized** and **saved** as event it in the database
 */
@Component
open class HibernateInsertEventListener(@Transient private val eventCollector: EventCollector) :
    PersistEventListener, PreInsertEventListener {

  @Throws(HibernateException::class)
  override fun onPersist(event: PersistEvent) {
    LOGGER.debug("Persist Entity: {}", event.getObject().javaClass.simpleName)
    eventCollector.collectKafkaStreamable(event.getObject())
  }

  @Throws(HibernateException::class)
  override fun onPersist(event: PersistEvent, createdAlready: PersistContext?) {
    // The entity has already gone through the persist cycle, so the event is already stored.
    LOGGER.debug("Persist Entity: {}, but created already", event.getObject().javaClass.simpleName)
  }

  // event.entity is no longer deprecated in Hibernate 5.6 and therefore was probably falsely
  // deprecated.
  @Suppress("DEPRECATION")
  override fun onPreInsert(event: PreInsertEvent): Boolean {
    LOGGER.debug("Pre Insert Entity: {}", event.entity.javaClass.simpleName)
    eventCollector.saveKafkaStreamable(event.entity)
    return false
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(HibernateInsertEventListener::class.java)
  }
}
