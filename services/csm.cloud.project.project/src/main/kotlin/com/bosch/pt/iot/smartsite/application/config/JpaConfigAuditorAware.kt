/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.application.config

import com.bosch.pt.iot.smartsite.application.security.SpringSecurityUserAuditorAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@Profile("!restore-db & !restore-db-test & !test-without-hibernate-listener")
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
open class JpaConfigAuditorAware {

  @Bean
  open fun auditorProvider() = SpringSecurityUserAuditorAware()
}
