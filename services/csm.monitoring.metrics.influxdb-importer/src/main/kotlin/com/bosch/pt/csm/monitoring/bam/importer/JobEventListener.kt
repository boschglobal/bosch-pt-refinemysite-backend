package com.bosch.pt.csm.monitoring.bam.importer

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.job.event.listener.JobEventListener
import com.bosch.pt.csm.cloud.job.messages.JobCompletedEventAvro
import com.bosch.pt.csm.cloud.job.messages.JobFailedEventAvro
import com.bosch.pt.csm.cloud.job.messages.JobQueuedEventAvro
import com.bosch.pt.csm.cloud.job.messages.JobRejectedEventAvro
import com.bosch.pt.csm.cloud.job.messages.JobStartedEventAvro
import com.bosch.pt.csm.cloud.job.messages.JsonSerializedObjectAvro
import com.bosch.pt.csm.monitoring.bam.importer.JobEventListener.JobStatus.COMPLETED
import com.bosch.pt.csm.monitoring.bam.importer.JobEventListener.JobStatus.FAILED
import com.bosch.pt.csm.monitoring.bam.importer.JobEventListener.JobStatus.QUEUED
import com.bosch.pt.csm.monitoring.bam.importer.JobEventListener.JobStatus.REJECTED
import com.bosch.pt.csm.monitoring.bam.importer.JobEventListener.JobStatus.STARTED
import com.bosch.pt.csm.monitoring.bam.importer.tables.ActingUsersTable
import com.fasterxml.jackson.databind.ObjectMapper
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Profile("!kafka-job-listener-disabled")
@Component
class JobEventListener(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
    namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {

  private val actingUsersTable = ActingUsersTable(namedParameterJdbcTemplate, "job-event")

  @Trace
  @KafkaListener(topics = ["\${custom.kafka.job-event-topic}"])
  @Transactional
  fun listenToJobEvents(record: ConsumerRecord<SpecificRecordBase, SpecificRecordBase>) {
    when (val event = record.value()) {
      is JobRejectedEventAvro -> handle(event)
      is JobQueuedEventAvro -> handle(event)
      is JobStartedEventAvro -> handle(event)
      is JobCompletedEventAvro -> handle(event)
      is JobFailedEventAvro -> handle(event)
      else -> LOGGER.debug("No handler found for ${event.javaClass.simpleName}")
    }
    actingUsersTable.ingestActingUser(record.value())
  }

  private fun handle(event: JobRejectedEventAvro) =
      jdbcTemplate.update(
          "INSERT INTO jobs VALUES (to_timestamp(?),?,?,?,?) ON CONFLICT DO NOTHING",
          event.getTimestamp().toInstantByMillis().epochSecond,
          event.getAggregateIdentifier().getIdentifier(),
          event.getJobType(),
          REJECTED.name,
          event.getJsonSerializedContext().getProjectIdentifier(),
      )

  private fun handle(event: JobQueuedEventAvro) =
      jdbcTemplate.update(
          "INSERT INTO jobs VALUES (to_timestamp(?),?,?,?,?) ON CONFLICT DO NOTHING",
          event.getTimestamp().toInstantByMillis().epochSecond,
          event.getAggregateIdentifier().getIdentifier(),
          event.getJobType(),
          QUEUED.name,
          event.getJsonSerializedContext().getProjectIdentifier(),
      )

  private fun handle(event: JobStartedEventAvro) =
      jdbcTemplate.update(
          "UPDATE jobs SET status = ? WHERE job_identifier = ? and status = ?",
          STARTED.name,
          event.getAggregateIdentifier().getIdentifier(),
          QUEUED.name)

  private fun handle(event: JobCompletedEventAvro) =
      jdbcTemplate.update(
          "UPDATE jobs " +
              "SET status = ?, duration_seconds = EXTRACT(EPOCH FROM to_timestamp(?) - created_at) " +
              "WHERE job_identifier = ? and status = ?",
          COMPLETED.name,
          event.getTimestamp().toInstantByMillis().epochSecond,
          event.getAggregateIdentifier().getIdentifier(),
          STARTED.name)

  private fun handle(event: JobFailedEventAvro) =
      jdbcTemplate.update(
          "UPDATE jobs SET status = ? WHERE job_identifier = ? and status = ?",
          FAILED.name,
          event.getAggregateIdentifier().getIdentifier(),
          STARTED.name)

  private fun JsonSerializedObjectAvro.getProjectIdentifier() =
      objectMapper.readTree(this.getJson()).get("project")?.get("id")?.asText()

  enum class JobStatus {
    REJECTED,
    QUEUED,
    STARTED,
    COMPLETED,
    FAILED
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(JobEventListener::class.java)
  }
}
