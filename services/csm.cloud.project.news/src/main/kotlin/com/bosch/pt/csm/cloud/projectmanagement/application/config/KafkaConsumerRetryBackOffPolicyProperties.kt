/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "custom.kafka.consumer.retry-back-off-policy")
open class KafkaConsumerRetryBackOffPolicyProperties(
    @JvmField final val initialDelayMs: Int = 1000,
    @JvmField final val maxDelayMs: Int = 60000,
    @JvmField final val multiplier: Int = 2,
    @JvmField final val retriesMax: Int = 5
)
