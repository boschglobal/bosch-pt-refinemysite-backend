/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.AlternativeJdkIdGenerator

@Configuration
class IdGeneratorConfiguration {

  @Bean fun idGenerator() = AlternativeJdkIdGenerator()
}
