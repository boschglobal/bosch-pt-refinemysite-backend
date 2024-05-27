/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.monitoring.bam.importer.config

import com.bosch.pt.csm.cloud.common.kafka.KafkaConsumerConfigUtil.configureDefaultErrorHandler
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.micrometer.core.instrument.MeterRegistry
import java.time.Duration
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.MicrometerConsumerListener
import org.springframework.kafka.support.serializer.RetryingDeserializer
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.RetryListener
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.util.backoff.ExponentialBackOff

@Profile("!test")
@Configuration
class KafkaConsumerConfiguration {

  @Bean
  fun kafkaConsumerFactory(
      kafkaProperties: KafkaProperties,
      meterRegistry: MeterRegistry,
      retryBackOffProperties: KafkaConsumerRetryBackOffPolicyProperties
  ) =
      DefaultKafkaConsumerFactory(
              kafkaProperties.buildConsumerProperties(null),
              RetryingDeserializer(
                  KafkaAvroDeserializer(), configureRetryTemplate(retryBackOffProperties)),
              RetryingDeserializer(
                  KafkaAvroDeserializer(), configureRetryTemplate(retryBackOffProperties)))
          .apply { addListener(MicrometerConsumerListener(meterRegistry)) }

  @Bean
  fun kafkaListenerContainerFactory(
      kafkaProperties: KafkaProperties,
      kafkaConsumerFactory: ConsumerFactory<in Any, in Any>,
      retryBackOffProperties: KafkaConsumerRetryBackOffPolicyProperties,
      @Value("\${custom.kafka.authExceptionRetryInterval:30s}") authExceptionRetryInterval: Duration
  ) =
      configureListenerFactory(
          kafkaProperties, kafkaConsumerFactory, retryBackOffProperties, authExceptionRetryInterval)

  private fun configureListenerFactory(
      kafkaProperties: KafkaProperties,
      kafkaConsumerFactory: ConsumerFactory<in Any, in Any>,
      retryBackOffProperties: KafkaConsumerRetryBackOffPolicyProperties,
      authExceptionRetryInterval: Duration
  ) =
      ConcurrentKafkaListenerContainerFactory<Any, Any>().apply {
        consumerFactory = kafkaConsumerFactory
        containerProperties.apply {
          isMissingTopicsFatal = false
          isFixTxOffsets = true
          this.authExceptionRetryInterval = authExceptionRetryInterval
        }
        isBatchListener = false
        setConcurrency(kafkaProperties.listener.concurrency)
        setCommonErrorHandler(configureErrorHandler(retryBackOffProperties))
      }

  private fun configureErrorHandler(
      retryBackOffProperties: KafkaConsumerRetryBackOffPolicyProperties,
  ) =
      configureDefaultErrorHandler(
          LOGGER,
          ExponentialBackOff().apply {
            initialInterval = retryBackOffProperties.initialDelayMs.toLong()
            multiplier = retryBackOffProperties.multiplier.toDouble()
            maxInterval = retryBackOffProperties.maxDelayMs.toLong()
          })

  private fun configureRetryTemplate(
      retryBackOffPolicyProperties: KafkaConsumerRetryBackOffPolicyProperties
  ) =
      RetryTemplate().apply {
        setBackOffPolicy(
            ExponentialBackOffPolicy().apply {
              initialInterval = retryBackOffPolicyProperties.initialDelayMs.toLong()
              multiplier = retryBackOffPolicyProperties.multiplier.toDouble()
              maxInterval = retryBackOffPolicyProperties.maxDelayMs.toLong()
            })
        setListeners(arrayOf<RetryListener>(DefaultRetryListener()))
        setRetryPolicy(
            SimpleRetryPolicy().apply { maxAttempts = retryBackOffPolicyProperties.retriesMax })
      }

  private class DefaultRetryListener : RetryListener {

    override fun <T, E : Throwable> open(context: RetryContext, callback: RetryCallback<T, E>) =
        true // let the retry proceed

    override fun <T, E : Throwable> close(
        context: RetryContext,
        callback: RetryCallback<T, E>,
        throwable: Throwable?
    ) = Unit

    override fun <T, E : Throwable> onError(
        context: RetryContext,
        callback: RetryCallback<T, E>,
        throwable: Throwable?
    ) = LOGGER.debug("Retry ${context.retryCount} failed.")
  }

  companion object {
    private val LOGGER = getLogger(KafkaConsumerConfiguration::class.java)
  }
}
