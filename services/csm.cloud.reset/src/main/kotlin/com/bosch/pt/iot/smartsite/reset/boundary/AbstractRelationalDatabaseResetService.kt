/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.reset.boundary

import com.bosch.pt.iot.smartsite.reset.jdbc.JdbcResetStrategy
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcOperations

@Suppress("UnnecessaryAbstractClass")
abstract class AbstractRelationalDatabaseResetService(
    private val resetStrategy: JdbcResetStrategy,
    private val jdbcOperations: JdbcOperations,
    private val serviceName: String
) {

  // Define the method as "open" to be proxyable by cglib
  open fun reset() {
    LOGGER.info("Reset {} database ...", serviceName)
    resetStrategy.executeReset(jdbcOperations)
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(AbstractRelationalDatabaseResetService::class.java)
  }
}
