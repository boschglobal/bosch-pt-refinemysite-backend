/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.reset.jdbc

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

interface JdbcResetStrategy {
  fun executeReset(jdbcOperations: JdbcOperations)
}

@Component("postgresResetStrategy")
class PostgresJdbcResetStrategy(private val environment: Environment) : JdbcResetStrategy {
  override fun executeReset(jdbcOperations: JdbcOperations) {
    val statements = mutableListOf<String>()
    try {
      jdbcOperations
          .query("SELECT * FROM information_schema.tables WHERE table_schema = current_schema()") {
              resultSet,
              _ ->
            resultSet.getString("table_name")
          }
          .filter { it !in ignoredTables() }
          .forEach { statements.add(buildStatement(it)) }

      jdbcOperations.batchUpdate(*statements.toTypedArray())
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      LOGGER.error("Unable to execute database reset. Used statements: {}", statements)
      LOGGER.error(e.message)
    }
  }

  private fun buildStatement(tableName: String): String =
      if (environment.acceptsProfiles(Profiles.of("kubernetes"))) "DROP TABLE $tableName CASCADE;"
      else "TRUNCATE TABLE $tableName CASCADE;"

  private fun ignoredTables(): List<String> =
      if (environment.acceptsProfiles(Profiles.of("kubernetes")))
          listOf("pg_stat_statements", "pg_buffercache", "pg_stat_statements_info")
      else
          listOf(
              "flyway_schema_history",
              "pg_stat_statements",
              "pg_buffercache",
              "pg_stat_statements_info")

  companion object {
    private val LOGGER = LoggerFactory.getLogger(PostgresJdbcResetStrategy::class.java)
  }
}

@Component
@Primary
class MySqlJdbcResetStrategy(private val environment: Environment) : JdbcResetStrategy {

  @Transactional
  override fun executeReset(jdbcOperations: JdbcOperations) {
    val statements = mutableListOf("SET FOREIGN_KEY_CHECKS = 0;")
    try {
      jdbcOperations
          .query("SELECT * FROM information_schema.tables WHERE table_schema = DATABASE()") {
              resultSet,
              _ ->
            resultSet.getString("table_name")
          }
          .filter { it !in ignoredTables() }
          .forEach { statements.add(buildStatement(it)) }
      statements.add("SET FOREIGN_KEY_CHECKS = 1;")

      jdbcOperations.batchUpdate(*statements.toTypedArray())
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      LOGGER.error("Unable to execute database reset. Used statements: {}", statements)
      LOGGER.error(e.message)
    }
  }

  private fun buildStatement(tableName: String): String =
      if (environment.acceptsProfiles(Profiles.of("kubernetes"))) "DROP TABLE $tableName"
      else "TRUNCATE TABLE $tableName"

  private fun ignoredTables(): List<String> =
      if (environment.acceptsProfiles(Profiles.of("kubernetes"))) emptyList()
      else listOf("flyway_schema_history")

  companion object {
    private val LOGGER = LoggerFactory.getLogger(MySqlJdbcResetStrategy::class.java)
  }
}
