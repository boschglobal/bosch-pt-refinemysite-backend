package com.bosch.pt.csm.monitoring.bam.importer

import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.monitoring.bam.importer.tables.ActingUsersTable
import com.bosch.pt.csm.monitoring.bam.importer.tables.EventsTable
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Profile("!kafka-user-listener-disabled")
@Component
class UserEventListener(
    jdbcTemplate: JdbcTemplate,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) : BaseEventListener(jdbcTemplate, EventsTable(namedParameterJdbcTemplate)) {

  private val actingUsersTable = ActingUsersTable(namedParameterJdbcTemplate, "user")

  @Trace
  @KafkaListener(topics = ["\${custom.kafka.user-topic}"])
  @Transactional
  fun listenToUserEvents(record: ConsumerRecord<SpecificRecordBase, SpecificRecordBase?>) {
    if (!record.isTombstone() && record.isAggregateEvent()) {
      writeEvent(record.key() as MessageKeyAvro, record.value()!!, "USER")
    }
    actingUsersTable.ingestActingUser(record.value())
    writeUser(record)
  }

  private fun writeUser(record: ConsumerRecord<SpecificRecordBase, SpecificRecordBase?>) {
    if (record.isAggregateEvent()) {
      val key = record.key() as MessageKeyAvro
      if (key.aggregateIdentifier.type == UsermanagementAggregateTypeEnum.USER.value) {
        if (!record.isTombstone()) {
          val event = record.value()
          if (event is UserEventAvro) upsertUser(event)
        } else if (record.isTombstone()) redactUser(key.aggregateIdentifier.identifier)
      }
    }
  }

  private fun upsertUser(event: UserEventAvro) {
    namedParameterJdbcTemplate.update(
        """INSERT INTO user_meta (user_id, email)
                VALUES (:userId, :email)
                ON CONFLICT (user_id) DO UPDATE
                    SET email = :email"""
            .trimIndent(),
        MapSqlParameterSource()
            .addValue("userId", event.aggregate.aggregateIdentifier.identifier)
            .addValue("email", event.aggregate.email))
  }

  private fun redactUser(userId: String) {
    namedParameterJdbcTemplate.update(
        "UPDATE user_meta SET email = '<deleted>' WHERE user_id = :userId",
        MapSqlParameterSource().addValue("userId", userId))
  }
}
