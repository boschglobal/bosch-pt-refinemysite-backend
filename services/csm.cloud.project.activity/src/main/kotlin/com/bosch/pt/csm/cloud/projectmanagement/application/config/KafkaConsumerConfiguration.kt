/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import com.bosch.pt.csm.cloud.common.kafka.AvroToEventMessageKeyRecordInterceptor
import com.bosch.pt.csm.cloud.common.kafka.KafkaConsumerConfigUtil
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.micrometer.core.instrument.MeterRegistry
import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.MicrometerConsumerListener
import org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL
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
              kafkaProperties.buildConsumerProperties(),
              RetryingDeserializer(
                  KafkaAvroDeserializer().also {
                    it.configure(kafkaProperties.buildConsumerProperties(), true)
                  },
                  configureRetryTemplate(retryBackOffProperties)),
              RetryingDeserializer(
                  KafkaAvroDeserializer().also {
                    it.configure(kafkaProperties.buildConsumerProperties(), false)
                  },
                  configureRetryTemplate(retryBackOffProperties)))
          .apply { addListener(MicrometerConsumerListener(meterRegistry)) }

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
        setCommonErrorHandler(configureErrorHandler(retryBackOffProperties))
        setRecordInterceptor(AvroToEventMessageKeyRecordInterceptor())
        containerProperties.apply {
          ackMode = MANUAL
          isMissingTopicsFatal = false
          isFixTxOffsets = true
          this.authExceptionRetryInterval = authExceptionRetryInterval
        }
      }

  private fun configureErrorHandler(
      retryBackOffProperties: KafkaConsumerRetryBackOffPolicyProperties,
  ) =
      KafkaConsumerConfigUtil.configureDefaultErrorHandler(
          LOGGER,
          ExponentialBackOff().apply {
            initialInterval = retryBackOffProperties.initialDelayMs.toLong()
            multiplier = retryBackOffProperties.multiplier.toDouble()
            maxInterval = retryBackOffProperties.maxDelayMs.toLong()
          })

  private fun configureRetryTemplate(
      retryBackOffProperties: KafkaConsumerRetryBackOffPolicyProperties
  ) =
      RetryTemplate().apply {
        setBackOffPolicy(
            ExponentialBackOffPolicy().apply {
              initialInterval = retryBackOffProperties.initialDelayMs.toLong()
              multiplier = retryBackOffProperties.multiplier.toDouble()
              maxInterval = retryBackOffProperties.maxDelayMs.toLong()
            })
        setListeners(arrayOf<RetryListener>(DefaultRetryListener()))
        setRetryPolicy(
            SimpleRetryPolicy().apply { maxAttempts = retryBackOffProperties.retriesMax })
      }

  private class DefaultRetryListener : RetryListener {

    override fun <T, E : Throwable> close(
        context: RetryContext,
        callback: RetryCallback<T, E>,
        throwable: Throwable?
    ) {
      // nothing to do
    }

    override fun <T, E : Throwable> open(context: RetryContext, callback: RetryCallback<T, E>) =
        true // let the retry proceed

    override fun <T, E : Throwable> onError(
        context: RetryContext,
        callback: RetryCallback<T, E>,
        throwable: Throwable?
    ) {
      LOGGER.debug("Retry ${context.retryCount} failed.")
    }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(KafkaConsumerConfiguration::class.java)
  }
}
