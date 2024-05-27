/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.monitoring.bam.importer.tables

import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import java.time.Instant
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class EventsTable(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

  fun ingestAggregateEvent(key: MessageKeyAvro, value: SpecificRecordBase, context: String) {
    namedParameterJdbcTemplate.update(
        """INSERT INTO events
          |     (event_time,
          |      event_name,
          |      aggregate_type,
          |      aggregate_identifier,
          |      root_context_identifier,
          |      context,
          |      acting_user)
          |   VALUES
          |     (to_timestamp(:eventTime),
          |      :eventName,
          |      :aggregateType,
          |      :aggregateIdentifier,
          |      :rootContextIdentifier,
          |      :context,
          |      :actingUser)
          |   ON CONFLICT DO NOTHING"""
            .trimMargin(),
        MapSqlParameterSource()
            .addValue("eventTime", extractLastModifiedDateAsEpochSeconds(value))
            .addValue("eventName", extractEventName(value))
            .addValue("aggregateType", extractAggregateType(value))
            .addValue("aggregateIdentifier", extractAggregateIdentifier(value))
            .addValue("rootContextIdentifier", key.rootContextIdentifier)
            .addValue("context", context)
            .addValue("actingUser", extractUserIdOfActingUser(value)))
  }

  private fun extractLastModifiedDateAsEpochSeconds(event: SpecificRecordBase) =
      Instant.ofEpochMilli(
              event
                  .extract("aggregate")
                  .extract("auditingInformation")["lastModifiedDate"]
                  .toString()
                  .toLong())
          .epochSecond

  private fun extractEventName(event: SpecificRecordBase) = event["name"].toString()

  private fun extractAggregateType(event: SpecificRecordBase) =
      event.extract("aggregate").extract("aggregateIdentifier")["type"].toString()

  private fun extractAggregateIdentifier(event: SpecificRecordBase) =
      event.extract("aggregate").extract("aggregateIdentifier")["identifier"].toString()
}
