/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretogglemanagement.feature.autoconfig

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "custom.kafka.consumer.retry-back-off-policy")
data class KafkaConsumerRetryBackOffPolicyProperties(
    val initialDelayMs: Int = 1000,
    val maxDelayMs: Int = 60000,
    val multiplier: Int = 2,
    val retriesMax: Int = 5
)
