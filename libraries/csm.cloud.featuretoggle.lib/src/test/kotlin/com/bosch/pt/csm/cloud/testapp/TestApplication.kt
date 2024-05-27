/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.testapp

import com.bosch.pt.csm.cloud.common.security.CustomWebSecurityAutoConfiguration
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.autoconfig.FeaturetoggleAutoconfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

@ImportAutoConfiguration(FeaturetoggleAutoconfiguration::class)
@SpringBootApplication(exclude = [CustomWebSecurityAutoConfiguration::class])
class TestApplication {
  @Bean fun logger(): Logger = LoggerFactory.getLogger("TestLogger")
}
