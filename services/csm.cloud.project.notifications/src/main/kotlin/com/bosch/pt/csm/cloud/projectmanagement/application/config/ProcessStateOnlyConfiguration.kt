/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import javax.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@EnableConfigurationProperties(ProcessStateOnlyProperties::class)
@Profile("!test")
class ProcessStateOnlyConfiguration(val processStateOnlyProperties: ProcessStateOnlyProperties) {

  @PostConstruct
  fun logOnInit() {
    if (processStateOnlyProperties.enabled) {
      LOGGER.warn(
          "Process state only is enabled. Events created before ${processStateOnlyProperties.untilDate}" +
              " will not produce notifications or events.")
    } else {
      LOGGER.info("Process state only is disabled.")
    }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(ProcessStateOnlyConfiguration::class.java)
  }
}
