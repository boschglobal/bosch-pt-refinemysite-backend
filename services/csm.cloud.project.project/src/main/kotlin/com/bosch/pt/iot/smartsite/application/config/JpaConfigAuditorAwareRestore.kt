/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.application.config

import com.bosch.pt.iot.smartsite.application.security.NoOpAuditorAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@Profile("restore-db", "restore-db-test", "test-without-hibernate-listener")
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider", modifyOnCreate = false, setDates = false)
open class JpaConfigAuditorAwareRestore {

  /**
   * When restoring the db from the event stream, jpa MUST NOT set auditing properties
   * automatically. They are taken over from the aggregates of the events. Therefore, we need to
   * configure one which does nothing.
   */
  @Bean
  open fun auditorProvider() = NoOpAuditorAware()
}
