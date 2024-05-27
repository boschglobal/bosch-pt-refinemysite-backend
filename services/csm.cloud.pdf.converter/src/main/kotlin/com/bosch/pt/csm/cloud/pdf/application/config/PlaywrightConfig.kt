/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.pdf.application.config

import com.bosch.pt.csm.cloud.pdf.application.pdf.PlaywrightFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles

@Configuration
class PlaywrightConfig(
    @Value("\${playwright.instances.initial:5}") private val initialInstances: Int
) {

  @Bean
  fun playwright(environment: Environment): PlaywrightFactory {
    if (!environment.acceptsProfiles(Profiles.of("local"))) {
      System.setProperty("playwright.cli.dir", "/tmp/playwright")
    }
    return PlaywrightFactory(initialInstances)
  }
}
