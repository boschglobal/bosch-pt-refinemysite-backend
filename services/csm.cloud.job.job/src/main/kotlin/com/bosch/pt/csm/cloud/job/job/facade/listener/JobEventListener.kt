/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.facade.listener

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.kafka.logConsumption
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.job.job.api.JobCompletedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobEvent
import com.bosch.pt.csm.cloud.job.job.api.JobFailedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobIdentifier
import com.bosch.pt.csm.cloud.job.job.api.JobQueuedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobRejectedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobResultReadEvent
import com.bosch.pt.csm.cloud.job.job.api.JobStartedEvent
import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import com.bosch.pt.csm.cloud.job.job.query.JobProjector
import com.bosch.pt.csm.cloud.job.messages.JobCompletedEventAvro
import com.bosch.pt.csm.cloud.job.messages.JobFailedEventAvro
import com.bosch.pt.csm.cloud.job.messages.JobQueuedEventAvro
import com.bosch.pt.csm.cloud.job.messages.JobRejectedEventAvro
import com.bosch.pt.csm.cloud.job.messages.JobResultReadEventAvro
import com.bosch.pt.csm.cloud.job.messages.JobStartedEventAvro
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class JobEventListener(private val jobProjector: JobProjector, private val logger: Logger) {

  @Trace
  @KafkaListener(
      topics = ["\${custom.kafka.bindings.job-event.kafkaTopic}"],
      groupId = "\${custom.kafka.listener.job-event.groupId}",
      clientIdPrefix = "\${custom.kafka.listener.job-event.clientIdPrefix}",
      concurrency = "\${custom.kafka.listener.job-event.concurrency}",
      containerFactory = "nonTransactionalKafkaListenerContainerFactory")
  fun listen(record: ConsumerRecord<EventMessageKey, SpecificRecord>) {
    logger.logConsumption(record)
    jobProjector.handle(toEvent(record.value()))
  }

  private fun toEvent(avro: SpecificRecord): JobEvent =
      when (avro) {
        is JobQueuedEventAvro -> toEvent(avro)
        is JobStartedEventAvro -> toEvent(avro)
        is JobCompletedEventAvro -> toEvent(avro)
        is JobFailedEventAvro -> toEvent(avro)
        is JobRejectedEventAvro -> toEvent(avro)
        is JobResultReadEventAvro -> toEvent(avro)
        else -> error("Unknown job event type: ${avro.javaClass.simpleName}")
      }

  private fun toEvent(avro: JobQueuedEventAvro): JobQueuedEvent =
      JobQueuedEvent(
          timestamp = avro.getTimestamp().toLocalDateTimeByMillis(),
          aggregateIdentifier = JobIdentifier(avro.getAggregateIdentifier().getIdentifier()),
          version = avro.getAggregateIdentifier().getVersion(),
          jobType = avro.getJobType(),
          userIdentifier = UserIdentifier(avro.getUserIdentifier()),
          serializedContext = avro.getJsonSerializedContext().toJsonSerializedObject(),
          serializedCommand = avro.getJsonSerializedCommand().toJsonSerializedObject())

  private fun toEvent(avro: JobStartedEventAvro): JobStartedEvent =
      JobStartedEvent(
          timestamp = avro.getTimestamp().toLocalDateTimeByMillis(),
          aggregateIdentifier = JobIdentifier(avro.getAggregateIdentifier().getIdentifier()),
          version = avro.getAggregateIdentifier().getVersion())

  private fun toEvent(avro: JobCompletedEventAvro): JobCompletedEvent =
      JobCompletedEvent(
          timestamp = avro.getTimestamp().toLocalDateTimeByMillis(),
          aggregateIdentifier = JobIdentifier(avro.getAggregateIdentifier().getIdentifier()),
          version = avro.getAggregateIdentifier().getVersion(),
          serializedResult = avro.getSerializedResult().toJsonSerializedObject())

  private fun toEvent(avro: JobFailedEventAvro): JobFailedEvent =
      JobFailedEvent(
          timestamp = avro.getTimestamp().toLocalDateTimeByMillis(),
          aggregateIdentifier = JobIdentifier(avro.getAggregateIdentifier().getIdentifier()),
          version = avro.getAggregateIdentifier().getVersion(),
      )

  private fun toEvent(avro: JobRejectedEventAvro): JobRejectedEvent =
      JobRejectedEvent(
          timestamp = avro.getTimestamp().toLocalDateTimeByMillis(),
          aggregateIdentifier = JobIdentifier(avro.getAggregateIdentifier().getIdentifier()),
          version = avro.getAggregateIdentifier().getVersion(),
          jobType = avro.getJobType(),
          userIdentifier = UserIdentifier(avro.getUserIdentifier()),
          serializedContext = avro.getJsonSerializedContext().toJsonSerializedObject())

  private fun toEvent(avro: JobResultReadEventAvro): JobResultReadEvent =
      JobResultReadEvent(
          timestamp = avro.getTimestamp().toLocalDateTimeByMillis(),
          aggregateIdentifier = JobIdentifier(avro.getAggregateIdentifier().getIdentifier()),
          version = avro.getAggregateIdentifier().getVersion(),
      )
}
