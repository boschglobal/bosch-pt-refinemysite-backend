/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.businesstransaction.autoconfiguration

import com.bosch.pt.csm.cloud.common.businesstransaction.EventOfBusinessTransactionRepositoryPort
import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.ConsumerBusinessTransactionManager
import com.bosch.pt.csm.cloud.common.businesstransaction.facade.listener.BusinessTransactionAware
import com.bosch.pt.csm.cloud.common.businesstransaction.jpa.JpaEventOfBusinessTransactionRepository
import com.bosch.pt.csm.cloud.common.businesstransaction.jpa.JpaEventOfBusinessTransactionRepositoryAdapter
import com.bosch.pt.csm.cloud.common.businesstransaction.metrics.BusinessTransactionMetrics
import com.bosch.pt.csm.cloud.common.businesstransaction.mongodb.MongoEventOfBusinessTransactionRepository
import com.bosch.pt.csm.cloud.common.businesstransaction.mongodb.MongoEventOfBusinessTransactionRepositoryAdapter
import com.bosch.pt.csm.cloud.common.businesstransaction.mongodb.MongoIndexConfiguration
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.MongoTransactionManager

@AutoConfiguration
@ConditionalOnProperty(prefix = "custom.business-transaction.consumer", value = ["persistence"])
class BusinessTransactionConsumerAutoConfiguration {

  @ConditionalOnMissingBean
  @Bean
  fun consumerBusinessTransactionManager(
      eventOfBusinessTransactionRepository: EventOfBusinessTransactionRepositoryPort
  ) = ConsumerBusinessTransactionManager(eventOfBusinessTransactionRepository)

  @ConditionalOnMissingBean
  @Bean
  fun businessTransactionMetrics(
      eventProcessors: List<BusinessTransactionAware>,
      eventOfBusinessTransactionRepository: EventOfBusinessTransactionRepositoryPort,
      meterRegistry: MeterRegistry
  ) =
      BusinessTransactionMetrics(
          eventProcessors, eventOfBusinessTransactionRepository, meterRegistry)

  @Configuration
  @ConditionalOnProperty(
      prefix = "custom.business-transaction.consumer", value = ["persistence"], havingValue = "jpa")
  @AutoConfigurationPackage(
      basePackages = ["com.bosch.pt.csm.cloud.common.businesstransaction.jpa"])
  class JpaAutoConfiguration {

    @ConditionalOnMissingBean
    @Bean
    fun eventOfBusinessTransactionRepositoryPort(
        jpaEventOfBusinessTransactionRepository: JpaEventOfBusinessTransactionRepository
    ) = JpaEventOfBusinessTransactionRepositoryAdapter(jpaEventOfBusinessTransactionRepository)
  }

  @Configuration
  @ConditionalOnProperty(
      prefix = "custom.business-transaction.consumer",
      value = ["persistence"],
      havingValue = "mongodb")
  @AutoConfigurationPackage(
      basePackages = ["com.bosch.pt.csm.cloud.common.businesstransaction.mongodb"])
  @Import(MongoIndexConfiguration::class)
  class MongoAutoConfiguration {

    @ConditionalOnMissingBean
    @Bean
    fun transactionManager(dbFactory: MongoDatabaseFactory): MongoTransactionManager =
        MongoTransactionManager(dbFactory)

    @ConditionalOnMissingBean
    @Bean
    fun eventOfBusinessTransactionRepositoryPort(
        mongoEventOfBusinessTransactionRepository: MongoEventOfBusinessTransactionRepository
    ) = MongoEventOfBusinessTransactionRepositoryAdapter(mongoEventOfBusinessTransactionRepository)
  }
}
