/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.application.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "custom.kafka.consumer.retry-back-off-policy")
class KafkaConsumerRetryBackOffPolicyProperties {
    var initialDelayMs = 1000
    var maxDelayMs = 60000
    var multiplier = 2
    var retriesMax = 5
}