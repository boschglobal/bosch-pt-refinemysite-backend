package com.bosch.pt.csm.monitoring.bam.importer

import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import com.bosch.pt.csm.monitoring.bam.importer.tables.ActingUsersTable
import com.bosch.pt.csm.monitoring.bam.importer.tables.EventsTable
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Profile("!kafka-company-listener-disabled")
@Component
class CompanyEventListener(
    jdbcTemplate: JdbcTemplate,
    namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) : BaseEventListener(jdbcTemplate, EventsTable(namedParameterJdbcTemplate)) {

  private val actingUsersTable = ActingUsersTable(namedParameterJdbcTemplate, "company")

  @Trace
  @KafkaListener(topics = ["\${custom.kafka.company-topic}"])
  @Transactional
  fun listenToCompanyEvents(record: ConsumerRecord<SpecificRecordBase, SpecificRecordBase?>) {
    if (!record.isTombstone() && record.isAggregateEvent()) {
      writeEvent(record.key() as MessageKeyAvro, record.value()!!, "COMPANY")
    }
    actingUsersTable.ingestActingUser(record.value())
  }
}
