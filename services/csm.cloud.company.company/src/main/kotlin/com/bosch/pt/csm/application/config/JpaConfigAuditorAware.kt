/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.application.config

import com.bosch.pt.csm.application.security.SpringSecurityAuditorAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

/** Configuration for JPA. */
@Profile("!restore-db & !restore-db-test")
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
class JpaConfigAuditorAware {

  /**
   * Add [org.springframework.data.domain.AuditorAware] bean to application context.
   *
   * @return auditor aware bean
   */
  @Bean fun auditorProvider(): SpringSecurityAuditorAware = SpringSecurityAuditorAware()
}
