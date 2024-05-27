/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.data.repository.query.SecurityEvaluationContextExtension

/** Configuration for spring security extension for spring data. */
@Configuration
open class SpringDataSecurityConfiguration {

  /**
   * Creates evaluation context for security expressions.
   *
   * @return security evaluation context
   */
  @Bean
  open fun securityEvaluationContextExtension(): SecurityEvaluationContextExtension =
      SecurityEvaluationContextExtension()
}
