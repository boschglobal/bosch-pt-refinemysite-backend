package com.bosch.pt.csm.cloud.featuretogglemanagement.feature.autoconfig

import com.bosch.pt.csm.cloud.common.kafka.KafkaConsumerConfigUtil.configureDefaultErrorHandler
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.facade.rest.FeatureController
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.query.FeatureProjector
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.query.FeatureQueryService
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.query.facade.listener.UpstreamFeatureEventListener
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
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

@AutoConfiguration
@AutoConfigurationPackage(basePackages = ["com.bosch.pt.csm.cloud.featuretogglemanagement.feature"])
@EnableConfigurationProperties(
    KafkaConsumerRetryBackOffPolicyProperties::class,
)
@Import(
    FeatureController::class,
    FeatureProjector::class,
    FeatureQueryService::class,
    UpstreamFeatureEventListener::class,
)
class FeaturetoggleAutoconfiguration {

  /**
   * Custom container factory for [UpstreamFeatureEventListener]. Needed to be independent of
   * configs of container factories of users of the library. E.g. a user of a library could have
   * configured manual ack mode which would not work with this library as the kafka listener method
   * does no manual acknowledgements.
   */
  @Bean
  @ConditionalOnBean(UpstreamFeatureEventListener::class)
  @ConditionalOnMissingBean(name = ["featuretoggleKafkaListenerContainerFactory"])
  fun featuretoggleKafkaListenerContainerFactory(
      kafkaProperties: KafkaProperties,
      kafkaConsumerRetryBackOffPolicyProperties: KafkaConsumerRetryBackOffPolicyProperties,
      logger: Logger,
      meterRegistry: MeterRegistry
  ): ConcurrentKafkaListenerContainerFactory<Any, Any> {
    val consumerFactory =
        DefaultKafkaConsumerFactory(
                kafkaProperties.buildConsumerProperties(),
                RetryingDeserializer(
                    KafkaAvroDeserializer(),
                    retryTemplate(kafkaConsumerRetryBackOffPolicyProperties, logger)),
                RetryingDeserializer(
                    KafkaAvroDeserializer(),
                    retryTemplate(kafkaConsumerRetryBackOffPolicyProperties, logger)))
            .apply { addListener(MicrometerConsumerListener(meterRegistry)) }

    return ConcurrentKafkaListenerContainerFactory<Any, Any>().apply {
      this.consumerFactory = consumerFactory
      // default AckMode.BATCH is sufficient
      containerProperties.apply {
        isMissingTopicsFatal = false
        isFixTxOffsets = true
      }
      setCommonErrorHandler(
          configureErrorHandler(logger, kafkaConsumerRetryBackOffPolicyProperties))
    }
  }

  private fun configureErrorHandler(
      logger: Logger,
      retryBackOffProperties: KafkaConsumerRetryBackOffPolicyProperties,
  ) =
      configureDefaultErrorHandler(
          logger,
          ExponentialBackOff().apply {
            initialInterval = retryBackOffProperties.initialDelayMs.toLong()
            multiplier = retryBackOffProperties.multiplier.toDouble()
            maxInterval = retryBackOffProperties.maxDelayMs.toLong()
          })

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
