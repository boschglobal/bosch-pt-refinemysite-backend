/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.kafka.eventlistener

import org.hibernate.HibernateException
import org.hibernate.event.spi.DeleteContext
import org.hibernate.event.spi.DeleteEvent
import org.hibernate.event.spi.DeleteEventListener
import org.hibernate.event.spi.PreDeleteEvent
import org.hibernate.event.spi.PreDeleteEventListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * This class supports the process of deleting an entity. <br></br> The [.onDelete] or [.onDelete]
 * is called before the [.onPreDelete].
 * * [DeleteEvent] is fired after [jakarta.persistence.EntityManager.remove] and the entity will be
 *   **collected** in the transaction
 * * [PreDeleteEvent] is fired before the real SQL `DELETE` statement and the entity will be
 *   **serialized** and **saved** as event it in the database
 */
@Component
open class HibernateDeleteEventListener(@Transient private val eventCollector: EventCollector) :
    DeleteEventListener, PreDeleteEventListener {

  @Throws(HibernateException::class)
  override fun onDelete(event: DeleteEvent) {
    LOGGER.debug("Delete Entity: {}", event.getObject().javaClass.simpleName)

    // Due to the implementation of JPA (em.remove(em.contains(entity) ? entity : em.merge(entity)))
    // it is possible that the entity has already been collected by the
    // HibernateUpdateEventListener#onMerge()
    if (!eventCollector.isEntityAlreadyStored(event.getObject())) {
      eventCollector.collectKafkaStreamable(event.getObject())
    }
  }

  @Throws(HibernateException::class)
  override fun onDelete(event: DeleteEvent, transientEntities: DeleteContext?) {
    LOGGER.debug("Delete Entity: {}", event.getObject().javaClass.simpleName)

    // Due to the implementation of JPA (em.remove(em.contains(entity) ? entity : em.merge(entity)))
    // it is possible that the entity has already been collected by the
    // HibernateUpdateEventListener#onMerge()
    if (!eventCollector.isEntityAlreadyStored(event.getObject())) {
      eventCollector.collectKafkaStreamable(event.getObject())
    }
  }

  // event.entity is no longer deprecated in Hibernate 5.6 and therefore was probably falsely
  // deprecated.
  @Suppress("DEPRECATION")
  override fun onPreDelete(event: PreDeleteEvent): Boolean {
    LOGGER.debug("Pre Delete Entity: {}", event.entity.javaClass.simpleName)
    eventCollector.saveKafkaStreamable(event.entity)
    return false
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(HibernateDeleteEventListener::class.java)
  }
}
