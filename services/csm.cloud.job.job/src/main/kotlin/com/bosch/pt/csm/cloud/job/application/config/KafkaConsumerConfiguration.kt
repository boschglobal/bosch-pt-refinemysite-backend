/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.application.config

import com.bosch.pt.csm.cloud.common.kafka.AvroToCommandMessageKeyRecordInterceptor
import com.bosch.pt.csm.cloud.common.kafka.AvroToEventMessageKeyRecordInterceptor
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.micrometer.core.instrument.MeterRegistry
import java.time.Duration
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.KafkaException.Level.ERROR
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.MicrometerConsumerListener
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.RetryingDeserializer
import org.springframework.kafka.transaction.KafkaTransactionManager
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.RetryListener
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.util.backoff.ExponentialBackOff

@Configuration
@EnableConfigurationProperties(
    KafkaTopicProperties::class, KafkaConsumerRetryBackOffPolicyProperties::class)
class KafkaConsumerConfiguration {

  @Bean
  fun kafkaConsumerFactory(
      kafkaProperties: KafkaProperties,
      kafkaConsumerRetryBackOffPolicyProperties: KafkaConsumerRetryBackOffPolicyProperties,
      logger: Logger,
      meterRegistry: MeterRegistry
  ) =
      DefaultKafkaConsumerFactory(
              kafkaProperties.buildConsumerProperties(),
              RetryingDeserializer(
                  KafkaAvroDeserializer().also {
                    it.configure(kafkaProperties.buildConsumerProperties(), true)
                  },
                  retryTemplate(kafkaConsumerRetryBackOffPolicyProperties, logger)),
              RetryingDeserializer(
                  KafkaAvroDeserializer().also {
                    it.configure(kafkaProperties.buildConsumerProperties(), false)
                  },
                  retryTemplate(kafkaConsumerRetryBackOffPolicyProperties, logger)))
          .apply { addListener(MicrometerConsumerListener(meterRegistry)) }

  @Bean
  fun transactionalKafkaListenerContainerFactory(
      kafkaConsumerFactory: ConsumerFactory<Any, Any>,
      kafkaTransactionManager: KafkaTransactionManager<*, *>,
      @Value("\${custom.kafka.authExceptionRetryInterval:30s}") authExceptionRetryInterval: Duration
  ): ConcurrentKafkaListenerContainerFactory<Any, Any> =
      ConcurrentKafkaListenerContainerFactory<Any, Any>().apply {
        consumerFactory = kafkaConsumerFactory
        // do not set AckMode.MANUAL on containerProperties because it will be ignored with
        // transactions
        containerProperties.apply {
          isMissingTopicsFatal = false
          transactionManager = kafkaTransactionManager
          isFixTxOffsets = true
          this.authExceptionRetryInterval = authExceptionRetryInterval
        }
        isBatchListener = false
        setCommonErrorHandler(configureErrorHandler())
        setRecordInterceptor(AvroToCommandMessageKeyRecordInterceptor())
      }

  @Bean
  fun nonTransactionalKafkaListenerContainerFactory(
      kafkaConsumerFactory: ConsumerFactory<Any, Any>
  ): ConcurrentKafkaListenerContainerFactory<Any, Any> =
      ConcurrentKafkaListenerContainerFactory<Any, Any>().apply {
        consumerFactory = kafkaConsumerFactory
        // default AckMode.BATCH is sufficient
        containerProperties.apply {
          isMissingTopicsFatal = false
          isFixTxOffsets = true
        }
        isBatchListener = false
        setCommonErrorHandler(configureErrorHandler())
        setRecordInterceptor(AvroToEventMessageKeyRecordInterceptor())
      }

  @Bean
  fun kafkaTransactionManager(@Qualifier("avro") producerFactory: ProducerFactory<Any, Any>) =
      KafkaTransactionManager(producerFactory)

  private fun configureErrorHandler() =
      DefaultErrorHandler(ExponentialBackOff()).apply { setLogLevel(ERROR) }

  private fun retryTemplate(
      kafkaConsumerRetryBackOffPolicyProperties: KafkaConsumerRetryBackOffPolicyProperties,
      logger: Logger
  ) =
      RetryTemplate().apply {
        setBackOffPolicy(
            ExponentialBackOffPolicy().apply {
              initialInterval = kafkaConsumerRetryBackOffPolicyProperties.initialDelayMs.toLong()
              multiplier = kafkaConsumerRetryBackOffPolicyProperties.multiplier.toDouble()
              maxInterval = kafkaConsumerRetryBackOffPolicyProperties.maxDelayMs.toLong()
            })
        setListeners(arrayOf<RetryListener>(DefaultRetryListener(logger)))
        setRetryPolicy(
            SimpleRetryPolicy().apply {
              maxAttempts = kafkaConsumerRetryBackOffPolicyProperties.retriesMax
            })
      }

  private class DefaultRetryListener(private val logger: Logger) : RetryListener {

    override fun <T, E : Throwable> open(context: RetryContext, callback: RetryCallback<T, E>) =
        true // let the retry proceed

    override fun <T, E : Throwable> close(
        context: RetryContext,
        callback: RetryCallback<T, E>,
        throwable: Throwable?
    ) {
      // nothing to do
    }

    override fun <T, E : Throwable> onError(
        context: RetryContext,
        callback: RetryCallback<T, E>,
        throwable: Throwable?
    ) {
      logger.debug("Retry ${context.retryCount} failed.")
    }
  }
}
