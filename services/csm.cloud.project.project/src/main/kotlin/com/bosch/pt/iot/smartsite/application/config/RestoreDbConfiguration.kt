/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.application.config

import com.bosch.pt.csm.cloud.common.streamable.restoredb.KafkaTopicOffsetSynchronizationManager
import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreDbStrategyDispatcher
import com.bosch.pt.iot.smartsite.company.facade.listener.restore.strategy.CompanyContextRestoreDbStrategy
import com.bosch.pt.iot.smartsite.craft.facade.listener.restore.strategy.CraftContextRestoreDbStrategy
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextRestoreDbStrategy
import com.bosch.pt.iot.smartsite.user.facade.listener.restore.strategy.UserContextRestoreDbStrategy
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Profile("restore-db")
@Component
class RestoreDbConfiguration {

  @Bean
  fun companyRestoreStrategyDispatcher(
      transactionTemplate: TransactionTemplate,
      strategies: List<CompanyContextRestoreDbStrategy>
  ): RestoreDbStrategyDispatcher<CompanyContextRestoreDbStrategy> =
      RestoreDbStrategyDispatcher(transactionTemplate, strategies)

  @Bean
  fun craftRestoreStrategyDispatcher(
      transactionTemplate: TransactionTemplate,
      strategies: List<CraftContextRestoreDbStrategy>
  ): RestoreDbStrategyDispatcher<CraftContextRestoreDbStrategy> =
      RestoreDbStrategyDispatcher(transactionTemplate, strategies)

  @Bean
  fun userRestoreStrategyDispatcher(
      transactionTemplate: TransactionTemplate,
      strategies: List<UserContextRestoreDbStrategy>
  ): RestoreDbStrategyDispatcher<UserContextRestoreDbStrategy> =
      RestoreDbStrategyDispatcher(transactionTemplate, strategies)

  @Bean
  fun projectRestoreStrategyDispatcher(
      transactionTemplate: TransactionTemplate,
      strategies: List<ProjectContextRestoreDbStrategy>
  ): RestoreDbStrategyDispatcher<ProjectContextRestoreDbStrategy> =
      RestoreDbStrategyDispatcher(transactionTemplate, strategies)

  @Bean
  @Profile("restore-db & !test")
  fun kafkaTopicOffsetSynchronizationManager(
      kafkaProperties: KafkaProperties,
      @Value("\${stage}") environment: String
  ): KafkaTopicOffsetSynchronizationManager =
      KafkaTopicOffsetSynchronizationManager(kafkaProperties, "csm-pm-$environment")
}
