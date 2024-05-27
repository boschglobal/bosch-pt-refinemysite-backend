package com.bosch.pt.csm.monitoring.bam.importer

import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventAvro
import com.bosch.pt.csm.monitoring.bam.importer.tables.ActingUsersTable
import com.bosch.pt.csm.monitoring.bam.importer.tables.EventsTable
import com.bosch.pt.csm.monitoring.bam.importer.tables.ProjectsTable
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Profile("!kafka-project-listener-disabled")
@Component
class ProjectEventListener(
    jdbcTemplate: JdbcTemplate,
    namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) : BaseEventListener(jdbcTemplate, EventsTable(namedParameterJdbcTemplate)) {

  private val actingUsersTable = ActingUsersTable(namedParameterJdbcTemplate, "project")
  private val projectsTable = ProjectsTable(namedParameterJdbcTemplate)

  @Trace
  @KafkaListener(topics = ["\${custom.kafka.project-topic}"])
  @Transactional
  fun listenToProjectEvents(record: ConsumerRecord<SpecificRecordBase, SpecificRecordBase?>) {
    if (!record.isTombstone() && record.isAggregateEvent()) {
      val key = record.key() as MessageKeyAvro
      val event = record.value()!!
      when (event) {
        is ProjectEventAvro -> projectsTable.ingestProject(event)
        is RelationEventAvro -> writeRelation(record.key() as MessageKeyAvro, event)
      }
      projectsTable.ingestProjectActivity(key.rootContextIdentifier, event)
    }
    writeEvent(record.key(), record.value()!!, "PROJECT")
    actingUsersTable.ingestActingUser(record.value())
  }

  private fun writeRelation(key: MessageKeyAvro, event: RelationEventAvro) {
    jdbcTemplate.update(
        "INSERT INTO relations VALUES(to_timestamp(?),?,?,?,?,?) ON CONFLICT DO NOTHING",
        extractLastModifiedDateAsEpochSeconds(event),
        event.getName().name,
        event.getIdentifier().toString(),
        key.getRootContextIdentifier(),
        event.getAggregate().getType().name,
        "${event.getAggregate().getSource().getIdentifier()}-${event.getAggregate().getTarget().getIdentifier()}")
  }
}
