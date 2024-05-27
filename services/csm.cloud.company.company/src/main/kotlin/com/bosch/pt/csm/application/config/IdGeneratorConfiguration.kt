/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.AlternativeJdkIdGenerator
import org.springframework.util.IdGenerator

/** Configures the implementation of [org.springframework.util.IdGenerator]. */
@Configuration
class IdGeneratorConfiguration {

  /**
   * Creates new [IdGenerator].
   *
   * @return the id generator
   */
  @Bean fun idGenerator(): IdGenerator = AlternativeJdkIdGenerator()
}
