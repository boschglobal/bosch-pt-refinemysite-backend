/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.kafka.eventlistener

import org.hibernate.HibernateException
import org.hibernate.event.spi.MergeContext
import org.hibernate.event.spi.MergeEvent
import org.hibernate.event.spi.MergeEventListener
import org.hibernate.event.spi.PreUpdateEvent
import org.hibernate.event.spi.PreUpdateEventListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * This class supports the process of updating an entity. <br></br> The [.onMerge] or [.onMerge] is
 * called before the [ ][.onPreUpdate].
 *
 * * [MergeEvent] is fired after [jakarta.persistence.EntityManager.merge] and the entity will be
 * **collected** in the transaction
 * * [PreUpdateEvent] is fired before the real SQL `UPDATE` statement and the entity will be
 * **serialized** and **saved** as event it in the database
 */
@Component
open class HibernateUpdateEventListener(@Transient private val eventCollector: EventCollector) :
    MergeEventListener, PreUpdateEventListener {

  @Throws(HibernateException::class)
  override fun onMerge(event: MergeEvent) {
    LOGGER.debug("Merge Entity: {}", event.original.javaClass.simpleName)
    eventCollector.collectKafkaStreamable(event.original)
  }

  @Throws(HibernateException::class)
  override fun onMerge(event: MergeEvent, copiedAlready: MergeContext?) {
    // The entity has already gone through the merge cycle, so the event is already stored.
    LOGGER.debug("Merge Entity: {}, but copied already", event.original.javaClass.simpleName)
  }

  // event.entity is no longer deprecated in Hibernate 5.6 and therefore was probably falsely
  // deprecated.
  @Suppress("DEPRECATION")
  override fun onPreUpdate(event: PreUpdateEvent): Boolean {
    LOGGER.debug("Pre Update Entity: {}", event.entity.javaClass.simpleName)
    eventCollector.saveKafkaStreamable(event.entity)
    return false
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(HibernateUpdateEventListener::class.java)
  }
}
