/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.application.config

import com.bosch.pt.iot.smartsite.common.kafka.eventlistener.HibernateDeleteEventListener
import com.bosch.pt.iot.smartsite.common.kafka.eventlistener.HibernateInsertEventListener
import com.bosch.pt.iot.smartsite.common.kafka.eventlistener.HibernateUpdateEventListener
import com.bosch.pt.iot.smartsite.common.util.returnUnit
import jakarta.annotation.PostConstruct
import jakarta.persistence.EntityManagerFactory
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.event.service.spi.EventListenerRegistry
import org.hibernate.event.spi.EventType.DELETE
import org.hibernate.event.spi.EventType.MERGE
import org.hibernate.event.spi.EventType.PERSIST
import org.hibernate.event.spi.EventType.PRE_DELETE
import org.hibernate.event.spi.EventType.PRE_INSERT
import org.hibernate.event.spi.EventType.PRE_UPDATE
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("!restore-db & !restore-db-test & !test-without-hibernate-listener")
@Configuration
open class HibernateEventListenerConfiguration(
    private val entityManagerFactory: EntityManagerFactory,
    private val insertEventListener: HibernateInsertEventListener,
    private val updateEventListener: HibernateUpdateEventListener,
    private val deleteEventListener: HibernateDeleteEventListener
) {

  @PostConstruct
  fun registerListeners(): Unit =
      entityManagerFactory
          .unwrap(SessionFactoryImplementor::class.java)
          .apply {
            checkNotNull(serviceRegistry.getService(EventListenerRegistry::class.java))
                .prependListeners(PERSIST, insertEventListener)

            checkNotNull(serviceRegistry.getService(EventListenerRegistry::class.java))
                .prependListeners(MERGE, updateEventListener)

            checkNotNull(serviceRegistry.getService(EventListenerRegistry::class.java))
                .prependListeners(DELETE, deleteEventListener)

            checkNotNull(serviceRegistry.getService(EventListenerRegistry::class.java))
                .prependListeners(PRE_INSERT, insertEventListener)

            checkNotNull(serviceRegistry.getService(EventListenerRegistry::class.java))
                .prependListeners(PRE_UPDATE, updateEventListener)

            checkNotNull(serviceRegistry.getService(EventListenerRegistry::class.java))
                .prependListeners(PRE_DELETE, deleteEventListener)
          }
          .returnUnit()
}
