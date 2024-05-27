/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.kafka.messaging.impl

import com.bosch.pt.csm.cloud.common.model.key.CommandMessageKey
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.job.event.submitJobQueuedEvent
import com.bosch.pt.csm.cloud.job.messages.EnqueueJobCommandAvro
import com.bosch.pt.iot.smartsite.application.config.KafkaTopicProperties.Companion.JOB_COMMAND_BINDING
import com.bosch.pt.iot.smartsite.common.kafka.messaging.CommandSendingService
import java.time.Instant
import jakarta.annotation.PostConstruct
import org.apache.avro.specific.SpecificRecord
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

/**
 * A test double of [CommandSendingServiceImpl] that allows to simulate external services as well as
 * to capture and verify sent messages.
 */
@Service
@Lazy
class CommandSendingServiceTestDouble(private val context: ApplicationContext) :
    CommandSendingService {

  private var eventStreamGenerator: EventStreamGenerator? = null

  /** When set to true, job events are generated based on the job commands sent */
  private var jobServiceSimulationActive = false

  val capturedRecords = mutableListOf<Record>()

  @PostConstruct
  @Suppress("SwallowedException")
  fun wireEventStreamGeneratorIfAvailable() {
    try {
      eventStreamGenerator = context.getBean(EventStreamGenerator::class.java)
    } catch (ex: NoSuchBeanDefinitionException) {
      jobServiceSimulationActive = false
    }
  }

  override fun send(key: CommandMessageKey, value: SpecificRecord, channel: String) {
    send(key.toAvro(), value, channel)
  }

  private fun send(key: SpecificRecord, value: SpecificRecord, channel: String) {
    capturedRecords.add(Record(key, value))
    if (jobServiceSimulationActive && channel == JOB_COMMAND_BINDING) {
      simulateJobService(value)
    }
  }

  fun clearRecords() = capturedRecords.clear()

  fun activateJobServiceSimulation() {
    if (eventStreamGenerator != null) {
      jobServiceSimulationActive = true
    } else {
      throw IllegalStateException(
          "You have tried to activate the job service simulation " +
              "but there is not bean of type EventStreamGenerator in the context")
    }
  }

  private fun simulateJobService(value: SpecificRecord) {
    when (value) {
      is EnqueueJobCommandAvro -> submitJobQueuedEvent(value)
    }
  }

  private fun submitJobQueuedEvent(value: EnqueueJobCommandAvro) {
    eventStreamGenerator!!.submitJobQueuedEvent("myJob") {
      it.aggregateIdentifier = value.getAggregateIdentifier()
      it.jobType = value.getJobType()
      it.userIdentifier = value.getUserIdentifier()
      it.timestamp = Instant.now().toEpochMilli()
      it.jsonSerializedContext = value.getJsonSerializedContext()
      it.jsonSerializedCommand = value.getJsonSerializedCommand()
    }
  }

  data class Record(val key: SpecificRecord, val value: SpecificRecord)
}
