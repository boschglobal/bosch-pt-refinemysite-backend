/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.config

import com.bosch.pt.iot.smartsite.application.security.SpringSecurityUuidAuditorAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.EnableMongoAuditing

@Configuration
@EnableMongoAuditing(auditorAwareRef = "mongoAuditorProvider")
open class MongoConfigurationAuditorAware {

  @Bean open fun mongoAuditorProvider() = SpringSecurityUuidAuditorAware()
}
