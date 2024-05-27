/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.reset.boundary

import com.bosch.pt.iot.smartsite.application.config.properties.CustomKafkaProperties
import com.bosch.pt.iot.smartsite.reset.Resettable
import java.time.Duration
import java.util.concurrent.TimeUnit
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.KafkaFuture
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.utils.Utils.sleep
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("!keep-topics")
class KafkaTopicsResetService(
    private val kafkaProperties: KafkaProperties,
    private val customKafkaProperties: CustomKafkaProperties
) : Resettable {

  override fun reset() {
    LOGGER.info("Reset kafka topics ...")

    val adminClient = createAdminClient()
    deleteTopics(adminClient)
    // Deletion of topics may takes several seconds after success is returned
    // https://kafka.apache.org/10/javadoc/org/apache/kafka/clients/admin/AdminClient.html
    // #deleteTopics-java.util.Collection-org.apache.kafka.clients.admin.DeleteTopicsOptions-
    sleep(10000)
    createTopics(adminClient)
    adminClient.close(Duration.ofSeconds(10))
  }

  private fun deleteTopics(adminClient: AdminClient) {
    val existingTopics = listExistingTopics(adminClient)
    val topicsToDelete =
        existingTopics
            .filter { anyMatchesPrefix(it, customKafkaProperties.getTopicPrefixes()) }
            .toSet()

    val deleteTopicsResult = adminClient.deleteTopics(topicsToDelete)
    deleteTopicsResult.topicNameValues().forEach { (s: String, future: KafkaFuture<Void>) ->
      futureWait(s, future, "deleted")
    }
  }

  private fun anyMatchesPrefix(prefix: String, names: Collection<String>): Boolean =
      names.any { it.startsWith(prefix) }

  private fun listExistingTopics(adminClient: AdminClient): Set<String> =
      try {
        adminClient.listTopics().names()[5, TimeUnit.SECONDS]
      } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        LOGGER.error("Topics could not be listed. Exception: {}", e.message)
        emptySet()
      }

  private fun createTopics(adminClient: AdminClient) {
    val createTopicsResult = adminClient.createTopics(topics)
    createTopicsResult.values().forEach { (s: String, future: KafkaFuture<Void>) ->
      futureWait(s, future, "created")
    }
  }

  private fun futureWait(s: String, future: KafkaFuture<Void>, action: String) =
      try {
        future[5, TimeUnit.SECONDS]
        LOGGER.info("Topic {} {}.", s, action)
      } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        LOGGER.info("Topic {} could not be {}. Exception: {}", s, action, e.message)
      }

  private fun createAdminClient(): AdminClient {
    val configs: MutableMap<String, Any?> = HashMap()
    configs[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaProperties.bootstrapServers
    addPropertyIfSet(configs, CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG)
    addPropertyIfSet(configs, CommonClientConfigs.SECURITY_PROTOCOL_CONFIG)
    addPropertyIfSet(configs, SaslConfigs.SASL_JAAS_CONFIG)
    addPropertyIfSet(configs, SaslConfigs.SASL_MECHANISM)
    addPropertyIfSet(configs, SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG)
    return AdminClient.create(configs)
  }

  private fun addPropertyIfSet(config: MutableMap<String, Any?>, property: String) {
    if (kafkaProperties.properties[property] != null) {
      config[property] = kafkaProperties.properties[property]
    }
  }

  private val topics: List<NewTopic>
    get() =
        customKafkaProperties.bindings.values.map {
          NewTopic(it.kafkaTopic, it.configuration.partitions, it.configuration.replication)
              .configs(it.configuration.properties)
        }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(KafkaTopicsResetService::class.java)
  }
}
