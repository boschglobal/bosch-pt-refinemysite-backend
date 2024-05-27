/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.kafkaconnector.config

import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.producer.ProducerConfig
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.transaction.ChainedTransactionManager
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.MicrometerProducerListener
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.transaction.KafkaTransactionManager
import org.springframework.transaction.support.AbstractPlatformTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION

@Configuration
@Profile("!test")
class KafkaConfiguration {

  @Bean
  fun kafkaTransactionManager(
      producerFactory: ProducerFactory<Any, Any>
  ): KafkaTransactionManager<*, *> {
    val kafkaTransactionManager = KafkaTransactionManager(producerFactory)
    kafkaTransactionManager.transactionSynchronization = SYNCHRONIZATION_ON_ACTUAL_TRANSACTION
    return kafkaTransactionManager
  }

  @Primary
  @Bean
  fun chainedTransactionManager(
      dataSourceTransactionManager: DataSourceTransactionManager,
      kafkaTransactionManager: KafkaTransactionManager<*, *>
  ) = ChainedTransactionManager(dataSourceTransactionManager, kafkaTransactionManager)

  @Bean
  fun kafkaProducerFactory(
      kafkaProperties: KafkaProperties,
      meterRegistry: MeterRegistry
  ): ProducerFactory<Any, Any> {
    val filteredConfig =
        kafkaProperties
            .buildProducerProperties()
            .entries
            .filter { (key) -> ProducerConfig.configNames().contains(key) }
            .associate { it.key to it.value }

    val producerFactory = DefaultKafkaProducerFactory<Any, Any>(filteredConfig)
    producerFactory.transactionIdPrefix = kafkaProperties.producer.transactionIdPrefix
    producerFactory.addListener(MicrometerProducerListener(meterRegistry))
    return producerFactory
  }
}
