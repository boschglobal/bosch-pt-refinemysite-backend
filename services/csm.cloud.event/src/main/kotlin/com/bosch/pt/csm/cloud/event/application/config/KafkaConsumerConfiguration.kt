/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.event.application.config

import io.micrometer.core.instrument.MeterRegistry
import java.time.Duration
import org.apache.commons.lang3.StringUtils
import org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.KafkaException.Level.ERROR
import org.springframework.kafka.KafkaException.Level.WARN
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.MicrometerConsumerListener
import org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.ExponentialBackOff
import org.springframework.util.backoff.FixedBackOff
import org.springframework.util.backoff.FixedBackOff.UNLIMITED_ATTEMPTS

@Profile("!test")
@Configuration
class KafkaConsumerConfiguration {

  @Value("\${pod.name:}") private val podName: String? = null

  @Bean
  fun kafkaConsumerFactory(
      kafkaProperties: KafkaProperties,
      meterRegistry: MeterRegistry
  ): ConsumerFactory<Any, Any>? {
    val configs = kafkaProperties.buildConsumerProperties()
    if (StringUtils.isNotBlank(podName)) {
      configs[GROUP_ID_CONFIG] = configs[GROUP_ID_CONFIG].toString() + "-" + podName
    }
    val factory = DefaultKafkaConsumerFactory<Any, Any>(configs)
    factory.addListener(MicrometerConsumerListener(meterRegistry))
    return factory
  }

  @Bean
  fun kafkaListenerContainerFactory(
      kafkaProperties: KafkaProperties,
      kafkaConsumerFactory: ConsumerFactory<in Any, in Any>,
      retryBackOffProperties: KafkaConsumerRetryBackOffPolicyProperties,
      @Value("\${custom.kafka.authExceptionRetryInterval:30s}") authExceptionRetryInterval: Duration
  ) =
      ConcurrentKafkaListenerContainerFactory<Any, Any>().apply {
        setConcurrency(kafkaProperties.listener.concurrency)
        consumerFactory = kafkaConsumerFactory
        setCommonErrorHandler(configureErrorHandler())
        configureErrorHandler(retryBackOffProperties)
        containerProperties.apply {
          ackMode = MANUAL
          isMissingTopicsFatal = false
          isFixTxOffsets = true
          this.authExceptionRetryInterval = authExceptionRetryInterval
        }
      }

  private fun configureErrorHandler() =
      DefaultErrorHandler(null, FixedBackOff(0, UNLIMITED_ATTEMPTS)).apply { setLogLevel(WARN) }

  private fun configureErrorHandler(
      retryBackOffProperties: KafkaConsumerRetryBackOffPolicyProperties,
  ) =
      DefaultErrorHandler(
              ExponentialBackOff().apply {
                initialInterval = retryBackOffProperties.initialDelayMs.toLong()
                multiplier = retryBackOffProperties.multiplier.toDouble()
                maxInterval = retryBackOffProperties.maxDelayMs.toLong()
              })
          .apply { setLogLevel(ERROR) }
}
