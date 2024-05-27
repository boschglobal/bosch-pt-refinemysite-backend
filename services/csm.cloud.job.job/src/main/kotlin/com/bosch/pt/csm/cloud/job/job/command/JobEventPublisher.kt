/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.command

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.kafka.logProduction
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import com.bosch.pt.csm.cloud.job.application.config.KafkaTopicProperties
import com.bosch.pt.csm.cloud.job.common.JobAggregateTypeEnum.JOB
import com.bosch.pt.csm.cloud.job.job.api.JobCompletedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobEvent
import com.bosch.pt.csm.cloud.job.job.api.JobFailedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobIdentifier
import com.bosch.pt.csm.cloud.job.job.api.JobQueuedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobRejectedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobResultReadEvent
import com.bosch.pt.csm.cloud.job.job.api.JobStartedEvent
import com.bosch.pt.csm.cloud.job.job.api.JsonSerializedObject
import com.bosch.pt.csm.cloud.job.messages.JobCompletedEventAvro
import com.bosch.pt.csm.cloud.job.messages.JobFailedEventAvro
import com.bosch.pt.csm.cloud.job.messages.JobQueuedEventAvro
import com.bosch.pt.csm.cloud.job.messages.JobRejectedEventAvro
import com.bosch.pt.csm.cloud.job.messages.JobResultReadEventAvro
import com.bosch.pt.csm.cloud.job.messages.JobStartedEventAvro
import com.bosch.pt.csm.cloud.job.messages.JsonSerializedObjectAvro
import java.nio.charset.StandardCharsets
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.utils.Utils
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class JobEventPublisher(
    private val kafkaTopicProperties: KafkaTopicProperties,
    @Qualifier("avro") private val kafkaTemplate: KafkaTemplate<MessageKeyAvro, SpecificRecord>,
    private val logger: Logger
) {

  fun publish(event: JobEvent) {
    sendDirect(event.aggregateIdentifier, event.version, event.toAvro())
  }

  private fun JobEvent.toAvro() =
      when (this) {
        is JobQueuedEvent -> this.toAvro()
        is JobRejectedEvent -> this.toAvro()
        is JobStartedEvent -> this.toAvro()
        is JobCompletedEvent -> this.toAvro()
        is JobFailedEvent -> this.toAvro()
        is JobResultReadEvent -> this.toAvro()
      }

  private fun JobQueuedEvent.toAvro() =
      JobQueuedEventAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              AggregateIdentifierAvro.newBuilder()
                  .setType(JOB.name)
                  .setIdentifier(aggregateIdentifier.value)
                  .setVersion(version))
          .setJobType(jobType)
          .setUserIdentifier(userIdentifier.value)
          .setTimestamp(timestamp.toEpochMilli())
          .setJsonSerializedContext(serializedContext?.toAvro())
          .setJsonSerializedCommand(serializedCommand.toAvro())
          .build()

  private fun JobStartedEvent.toAvro() =
      JobStartedEventAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              AggregateIdentifierAvro.newBuilder()
                  .setType(JOB.name)
                  .setIdentifier(aggregateIdentifier.value)
                  .setVersion(version))
          .setTimestamp(timestamp.toEpochMilli())
          .build()

  private fun JobCompletedEvent.toAvro() =
      JobCompletedEventAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              AggregateIdentifierAvro.newBuilder()
                  .setType(JOB.name)
                  .setIdentifier(aggregateIdentifier.value)
                  .setVersion(version))
          .setTimestamp(timestamp.toEpochMilli())
          .setSerializedResult(serializedResult.toAvro())
          .build()

  private fun JobFailedEvent.toAvro() =
      JobFailedEventAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              AggregateIdentifierAvro.newBuilder()
                  .setType(JOB.name)
                  .setIdentifier(aggregateIdentifier.value)
                  .setVersion(version))
          .setTimestamp(timestamp.toEpochMilli())
          .build()

  private fun JobRejectedEvent.toAvro() =
      JobRejectedEventAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              AggregateIdentifierAvro.newBuilder()
                  .setType(JOB.name)
                  .setIdentifier(aggregateIdentifier.value)
                  .setVersion(version))
          .setJobType(jobType)
          .setUserIdentifier(userIdentifier.value)
          .setTimestamp(timestamp.toEpochMilli())
          .setJsonSerializedContext(serializedContext?.toAvro())
          .build()

  private fun JobResultReadEvent.toAvro() =
      JobResultReadEventAvro.newBuilder()
          .setAggregateIdentifierBuilder(
              AggregateIdentifierAvro.newBuilder()
                  .setType(JOB.name)
                  .setIdentifier(aggregateIdentifier.value)
                  .setVersion(version))
          .setTimestamp(timestamp.toEpochMilli())
          .build()

  private fun JsonSerializedObject.toAvro() =
      JsonSerializedObjectAvro.newBuilder().setType(this.type).setJson(this.json).build()

  private fun sendDirect(jobIdentifier: JobIdentifier, version: Long, value: SpecificRecord) {
    val topic = kafkaTopicProperties.getTopicForChannel("job-event")
    val key =
        MessageKeyAvro.newBuilder()
            .setRootContextIdentifier(jobIdentifier.value)
            .setAggregateIdentifierBuilder(
                AggregateIdentifierAvro.newBuilder()
                    .setIdentifier(jobIdentifier.value)
                    .setType("Job")
                    .setVersion(version))
            .build()

    val producerRecord = ProducerRecord(topic, partitionOf(key, "job-event"), key, value)

    kafkaTemplate.send(producerRecord).get().also { logger.logProduction(it.producerRecord) }
  }

  private fun partitionOf(keyAvro: MessageKeyAvro, channel: String): Int {
    return (Utils.toPositive(
        Utils.murmur2(keyAvro.getRootContextIdentifier().toByteArray(StandardCharsets.UTF_8))) %
        kafkaTopicProperties.getConfigForChannel(channel).partitions)
  }
}
