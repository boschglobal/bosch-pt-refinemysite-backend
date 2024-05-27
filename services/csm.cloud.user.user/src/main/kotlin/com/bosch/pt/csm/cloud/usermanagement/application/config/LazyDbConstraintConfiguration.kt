/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.application.config

import java.sql.ResultSet
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextStartedEvent
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.jdbc.core.JdbcTemplate

@Configuration
class LazyDbConstraintConfiguration(
    private val environment: Environment,
    private val jdbcTemplate: JdbcTemplate
) : ApplicationListener<ContextStartedEvent> {

  override fun onApplicationEvent(event: ContextStartedEvent) {
    if (environment.acceptsProfiles(Profiles.of("restore-db"))) {
      dropIfExists(USER_ENTITY, "index", "UK_UserId")
      dropIfExists(USER_ENTITY, "index", "UK_User_Email")
    } else {
      addConstraintIfNotExists(USER_ENTITY, "UK_UserId", "UK_UserId unique (user_id)")
      addConstraintIfNotExists(USER_ENTITY, "UK_User_Email", "UK_User_Email unique (email)")
    }
  }

  private fun addConstraintIfNotExists(tableName: String, constraint: String, definition: String) {
    if (!indexExists(tableName, constraint)) {
      jdbcTemplate.execute("alter table $tableName add constraint $definition")
    }
  }

  private fun dropIfExists(tableName: String, type: String, name: String) {
    if (indexExists(tableName, name)) {
      jdbcTemplate.execute("drop $type $name on $tableName")
    }
  }

  private fun indexExists(tableName: String, indexName: String): Boolean =
      jdbcTemplate
          .queryForObject(
              "select COUNT(*) from information_schema.statistics where table_schema = DATABASE() " +
                  "and table_name = '$tableName' and INDEX_NAME = '$indexName'") {
              rs: ResultSet,
              _: Int ->
            rs.getLong(1)
          }
          .let { indexCount ->
            return@let indexCount != null && indexCount != 0L
          }

  companion object {
    private const val USER_ENTITY = "user_entity"
  }
}
