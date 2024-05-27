/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.kafkaconnector.schedule

import com.bosch.pt.csm.cloud.common.kafkaconnector.KafkaConnectorApplication
import com.bosch.pt.csm.cloud.common.kafkaconnector.data.EventDataService
import com.bosch.pt.csm.cloud.common.kafkaconnector.kafka.KafkaFeedService
import java.util.concurrent.atomic.AtomicBoolean
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.kafka.common.errors.ProducerFencedException
import org.apache.kafka.common.errors.UnknownProducerIdException
import org.slf4j.LoggerFactory.getLogger
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@Profile("!test")
class ScheduleService(
    private val eventDataService: EventDataService,
    private val kafkaFeedService: KafkaFeedService,
    private val env: Environment,
) {

  @Scheduled(fixedDelayString = "\${event.polling-delay}")
  fun run() {
    var moreDataAvailable: Boolean
    val unknownOrFencedProducer = AtomicBoolean()

    do {
      moreDataAvailable =
          eventDataService.eventTableNames().any { table ->
            try {
              return@any kafkaFeedService.feedBatch(table)
            } catch (@Suppress("TooGenericExceptionCaught") ex: Exception) {
              if (ExceptionUtils.getRootCause(ex) is UnknownProducerIdException) {
                LOGGER.warn("Cannot produce any longer because producer has unknown id")
                unknownOrFencedProducer.set(true)
                return@any false
              } else if (ExceptionUtils.getRootCause(ex) is ProducerFencedException) {
                LOGGER.warn("Cannot produce any longer because producer was fenced")
                unknownOrFencedProducer.set(true)
                return@any false
              } else {
                LOGGER.error("Unable to feed events from $table to Kafka: ${ex.message}", ex)
                return@any false
              }
            }
          }
    } while (!unknownOrFencedProducer.get() && moreDataAvailable)

    if (unknownOrFencedProducer.get()) {
      if (env.acceptsProfiles(Profiles.of("local-project", "local-company", "local-user"))) {
        LOGGER.warn("Restart the application...")
        KafkaConnectorApplication.restart()
      } else {
        LOGGER.warn("Stop the application...")
        KafkaConnectorApplication.stop()
      }
    }

    LOGGER.info("Schedule Service sleeping...")
  }

  companion object {
    private val LOGGER = getLogger(ScheduleService::class.java)
  }
}
