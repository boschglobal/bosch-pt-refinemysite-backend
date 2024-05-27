/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.application.config

import java.sql.ResultSet
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.event.ContextStartedEvent
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.jdbc.core.JdbcTemplate

/**
 * When restoring the database from the event stream, we can have data constellations in between
 * that do not fulfill some constraints defined on the database level. These will only eventually be
 * fulfilled once the restore is done or at least caught up with the message consumption. Therefore,
 * we have to deactivate some database constraints in restore mode and make sure they are enabled
 * again when in online mode.
 */
@Profile("!test")
@Configuration
open class LazyDbConstraintConfiguration(
    private val environment: Environment,
    private val jdbcTemplate: JdbcTemplate
) : ApplicationListener<ContextStartedEvent> {

  override fun onApplicationEvent(event: ContextStartedEvent) =
      if (environment.acceptsProfiles(Profiles.of("restore-db"))) {
        dropIndexIfExists("user_entity", "UK_UserId")
        dropIndexIfExists("user_entity", "UK_User_Email")
      } else {
        addConstraintIfNotExists("user_entity", "UK_UserId", "UK_UserId unique (user_id)")
        addConstraintIfNotExists("user_entity", "UK_User_Email", "UK_User_Email unique (email)")
      }

  private fun addConstraintIfNotExists(tableName: String, constraint: String, definition: String) {
    if (!indexExists(tableName, constraint)) {
      jdbcTemplate.execute("alter table $tableName add constraint $definition")
    }
  }

  private fun dropIndexIfExists(tableName: String, name: String) {
    if (indexExists(tableName, name)) {
      jdbcTemplate.execute("drop index $name on $tableName")
    }
  }

  private fun indexExists(tableName: String, indexName: String): Boolean {
    val indexCount =
        jdbcTemplate.queryForObject(
            "select COUNT(*) from information_schema.statistics where table_schema = DATABASE() " +
                "and table_name = '$tableName' and index_name = '$indexName'") {
            rs: ResultSet,
            _: Int ->
          rs.getLong(1)
        }
    return indexCount != null && indexCount != 0L
  }
}
