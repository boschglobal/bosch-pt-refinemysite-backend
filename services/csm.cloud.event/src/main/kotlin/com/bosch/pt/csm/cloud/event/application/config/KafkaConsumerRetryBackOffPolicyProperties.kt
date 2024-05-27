/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.event.application.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "custom.kafka.consumer.retry-back-off-policy")
data class KafkaConsumerRetryBackOffPolicyProperties(
    val initialDelayMs: Int = 1000,
    val maxDelayMs: Int = 60000,
    val multiplier: Int = 2,
    val retriesMax: Int = 5
)
