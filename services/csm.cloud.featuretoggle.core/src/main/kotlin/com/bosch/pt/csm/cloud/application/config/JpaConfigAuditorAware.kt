/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.application.config

import com.bosch.pt.csm.cloud.application.security.SpringSecurityAuditorAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
class JpaConfigAuditorAware {

  @Bean fun auditorProvider(): SpringSecurityAuditorAware = SpringSecurityAuditorAware()
}
