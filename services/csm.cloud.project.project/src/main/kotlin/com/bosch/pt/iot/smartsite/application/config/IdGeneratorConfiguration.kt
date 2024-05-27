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
import org.springframework.util.AlternativeJdkIdGenerator
import org.springframework.util.IdGenerator

@Configuration
open class IdGeneratorConfiguration {

  @Bean open fun idGenerator(): IdGenerator = AlternativeJdkIdGenerator()
}
