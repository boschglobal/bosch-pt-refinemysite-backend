package com.bosch.pt.csm.monitoring.bam.importer

import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import com.bosch.pt.csm.monitoring.bam.importer.tables.ActingUsersTable
import com.bosch.pt.csm.monitoring.bam.importer.tables.BimEventsTable
import com.bosch.pt.csm.monitoring.bam.importer.tables.BimModelsTable
import com.bosch.pt.csm.monitoring.bam.importer.tables.ProjectsTable
import datadog.trace.api.Trace
import java.time.Instant
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Profile("!kafka-bim-model-listener-disabled")
@Component
class BimModelEventListener(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

  private val actingUsersTable =
      ActingUsersTable(
          namedParameterJdbcTemplate,
          "bim-model",
          // We want to drop all events that occurred on the day of go live of bim.model on PROD,
          // because that's when we replicated a number of events from the project topic in the name
          // of the original users, causing a spike in acting users.
          Instant.parse("2022-11-09T00:00:00.000Z"))
  private val bimEventsTable = BimEventsTable(namedParameterJdbcTemplate)
  private val bimModelsTable = BimModelsTable(namedParameterJdbcTemplate)
  private val projectsTable = ProjectsTable(namedParameterJdbcTemplate)

  @Trace
  @KafkaListener(topics = ["\${custom.kafka.bim-model-topic}"])
  @Transactional
  fun listenToBimModelEvents(record: ConsumerRecord<SpecificRecordBase, SpecificRecordBase?>) {
    actingUsersTable.ingestActingUser(record.value())
    writeBimEvent(record)
    bimModelsTable.ingestBimModel(record)
  }

  private fun writeBimEvent(record: ConsumerRecord<SpecificRecordBase, SpecificRecordBase?>) {
    when (val key = record.key()) {
      is MessageKeyAvro ->
          record.value()?.let { event ->
            bimEventsTable.ingestBimEvent(key.rootContextIdentifier, event)
            projectsTable.ingestProjectActivity(key.rootContextIdentifier, event)
          }
    }
  }
}
