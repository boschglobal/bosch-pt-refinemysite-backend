/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.kafkaconnector.data

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.kafkaconnector.data.dto.Event
import java.sql.ResultSet
import java.util.regex.Pattern
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.stereotype.Service

@Service
class EventDataService(
    @Value("\${event.database-name}") private val database: String,
    @Value("\${event.batch-size}") private val batchSize: Int,
    @Value("\${event.query.table-names}") private val tableNameQuery: String,
    @Value("\${event.query.next-batch}") private val nextBatchQuery: String,
    @Value("\${event.query.remove-batch}") private val removeBatchQuery: String,
    @Qualifier("tablePattern") private val tableNameMatcher: Pattern,
    private val jdbcOperations: JdbcOperations
) {

  fun eventTableNames(): List<String> {
    val query = String.format(tableNameQuery, database)
    return jdbcOperations
        .query(query) { rs: ResultSet, _: Int -> rs.getString("table_name") }
        .filter { name -> tableNameMatcher.matcher(name).find() }
  }

  fun nextBatch(table: String): List<Event> =
      jdbcOperations.query(
          String.format(nextBatchQuery, table),
          { rs: ResultSet, _: Int ->
            Event(
                rs.getLong("id"),
                rs.getBytes("event_key"),
                rs.getBytes("event"),
                rs.getInt("partition_number"),
                rs.getString("trace_header_key"),
                rs.getString("trace_header_value"),
                rs.getString("transaction_identifier")?.toUUID())
          },
          batchSize)

  fun removeBatch(table: String, events: List<Event>): IntArray =
      jdbcOperations.batchUpdate(
          String.format(removeBatchQuery, table), events.map { event -> arrayOf<Any>(event.id) })
}
