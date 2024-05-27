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
import org.springframework.util.AlternativeJdkIdGenerator
import org.springframework.util.IdGenerator

@Configuration
class IdGeneratorConfiguration {

  @Bean fun idGenerator(): IdGenerator = AlternativeJdkIdGenerator()
}
