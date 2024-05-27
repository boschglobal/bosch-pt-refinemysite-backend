/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.kafka

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean

@AutoConfiguration
@ConditionalOnProperty(
    name = ["custom.kafka.dns.caching.enabled"], havingValue = "true", matchIfMissing = true)
open class JvmDnsCachingAutoConfiguration {

  @Bean(initMethod = "setDnsCachingSettings")
  open fun jvmDnsCachingSettings() = JvmDnsCachingSettings()
}
