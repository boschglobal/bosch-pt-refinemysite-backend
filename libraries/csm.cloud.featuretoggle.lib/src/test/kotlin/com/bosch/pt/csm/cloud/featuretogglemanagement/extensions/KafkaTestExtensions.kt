/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretogglemanagement.extensions

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName

private val kafkaBrokerDockerImageName = DockerImageName.parse("confluentinc/cp-kafka:7.2.1")

class JupiterKafkaTestExtension : BeforeAllCallback, AfterAllCallback {

  override fun beforeAll(context: ExtensionContext) {
    if (context.container == null) {
      context.container = bootKafkaContainer(logger)
    }
  }

  private var ExtensionContext.container: KafkaContainer?
    get() = getStore(GLOBAL).get("KAFKA_CONTAINER", KafkaContainer::class.java)
    set(value) = getStore(GLOBAL).put("KAFKA_CONTAINER", value)

  private companion object {
    val logger: Logger = LoggerFactory.getLogger(JupiterKafkaTestExtension::class.java)
  }

  override fun afterAll(context: ExtensionContext) {
    context.container?.stop()
  }
}

private fun bootKafkaContainer(logger: Logger): KafkaContainer {
  logger.info("Starting Kafka container ...")
  val kafkaContainer = KafkaContainer(kafkaBrokerDockerImageName)
  kafkaContainer.start()
  logger.info("Setting Spring bootstrap servers property to ${kafkaContainer.bootstrapServers} ...")
  System.setProperty("spring.kafka.bootstrap-servers", kafkaContainer.bootstrapServers)
  logger.info("Configuring Spring to use mock schema registry ...")
  System.setProperty("spring.kafka.properties.schema.registry.url", "mock://test-url")
  System.setProperty("spring.kafka.properties.auto.register.schemas", "true")
  return kafkaContainer
}
