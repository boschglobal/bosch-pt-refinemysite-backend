/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.data.repository.query.SecurityEvaluationContextExtension

/** Configuration for spring security extension for spring data. */
@Configuration
class SpringDataSecurityConfiguration {

  /**
   * Creates evaluation context for security expressions.
   *
   * @return security evaluation context
   */
  @Bean fun securityEvaluationContextExtension() = SecurityEvaluationContextExtension()
}
