/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.util

import jakarta.persistence.EntityManager
import org.hibernate.ReplicationMode.OVERWRITE
import org.hibernate.engine.spi.SessionImplementor

object JpaUtilities {

  fun <T> replicate(entityManager: EntityManager, entity: T) =
      entityManager.unwrap(SessionImplementor::class.java).replicate(entity, OVERWRITE)
}
