/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.monitoring.bam.importer.tables

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import java.time.Instant
import org.apache.avro.generic.GenericRecord
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class ActingUsersTable(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val topic: String,
    dropUntil: Instant = Instant.EPOCH
) {

  private val dropUntilEpochSeconds = dropUntil.epochSecond

  fun ingestActingUser(event: GenericRecord?) {
    if (event != null) {
      val eventTimeAsEpochSeconds = extractEventTimeAsEpochSeconds(event)
      if (dropUntilEpochSeconds <= eventTimeAsEpochSeconds)
          jdbcTemplate.update(
              """INSERT INTO acting_users
                        VALUES (to_timestamp(:eventTime), :eventName, :userId, :topic)
                        ON CONFLICT DO NOTHING"""
                  .trimIndent(),
              MapSqlParameterSource()
                  .addValue("eventTime", eventTimeAsEpochSeconds)
                  .addValue("eventName", extractEventName(event))
                  .addValue("userId", extractUserIdOfActingUser(event))
                  .addValue("topic", topic))
    }
  }

  private fun extractEventTimeAsEpochSeconds(event: GenericRecord): Long =
      if (event.hasField("auditingInformation"))
          event
              .extract("auditingInformation")["date"]
              .toString()
              .toLong()
              .toInstantByMillis()
              .epochSecond
      else if (event.hasField("aggregate"))
          event
              .extract("aggregate")
              .extract("auditingInformation")["lastModifiedDate"]
              .toString()
              .toLong()
              .toInstantByMillis()
              .epochSecond
      else 0

  private fun extractEventName(event: GenericRecord) = event.javaClass.simpleName
}
