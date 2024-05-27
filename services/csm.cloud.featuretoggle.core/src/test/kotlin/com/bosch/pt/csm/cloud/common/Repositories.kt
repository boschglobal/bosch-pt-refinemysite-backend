/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common

import com.bosch.pt.csm.cloud.user.query.UserProjectionRepository
import java.sql.ResultSet
import javax.sql.DataSource
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Profile("test")
@Service
@Lazy
class Repositories(
    val userProjectionRepository: UserProjectionRepository,
    val dataSource: DataSource
) {

  fun truncateDatabase() {
    truncateDatabaseFromSource(dataSource)
  }

  companion object {

    fun truncateDatabaseFromSource(source: DataSource) {
      val jdbcTemplate = JdbcTemplate(source)
      jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0;")
      jdbcTemplate
          .query(
              "SELECT * FROM information_schema.tables where table_schema = (SELECT DATABASE())") {
              rs: ResultSet,
              _ ->
            rs.getString("table_name")
          }
          .filter { it != "flyway_schema_history" }
          .forEach { jdbcTemplate.execute("truncate table $it") }
      jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1;")
    }
  }
}
