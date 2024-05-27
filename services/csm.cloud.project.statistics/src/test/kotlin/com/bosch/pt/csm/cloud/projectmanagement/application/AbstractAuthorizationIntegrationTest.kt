/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application

import com.bosch.pt.csm.cloud.projectmanagement.application.security.doWithAuthorization
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.User
import java.sql.ResultSet
import javax.sql.DataSource
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.access.AccessDeniedException

abstract class AbstractAuthorizationIntegrationTest {

  @Autowired private lateinit var dataSource: DataSource

  data class UserAccess(val alias: String, val user: User, val isAdmin: Boolean)

  protected fun checkAccessWith(
      accessList: List<String>,
      isGranted: Boolean,
      procedure: () -> Unit
  ) =
      accessList
          .map { getUserAccess(it) }
          .map {
            if (isGranted) {
              dynamicTest("${it.alias} is granted") {
                doWithAuthorization(it.user, it.isAdmin) { procedure() }
              }
            } else {
              dynamicTest("${it.alias} is denied") {
                assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
                  doWithAuthorization(it.user, it.isAdmin) { procedure() }
                }
              }
            }
          }

  abstract fun getUserAccess(alias: String): UserAccess

  fun truncateDatabase() {
    truncateDatabaseFromSource(dataSource)
  }

  companion object {

    fun truncateDatabaseFromSource(source: DataSource) {
      val queries = mutableListOf("SET FOREIGN_KEY_CHECKS = 0;")
      val jdbcTemplate = JdbcTemplate(source)
      jdbcTemplate
          .query("SELECT * FROM information_schema.tables where table_schema = DATABASE()") {
              rs: ResultSet,
              _ ->
            rs.getString("table_name")
          }
          .filter { it != "flyway_schema_history" }
          .forEach { queries.add("truncate table $it;") }
      queries.add("SET FOREIGN_KEY_CHECKS = 1;")

      jdbcTemplate.batchUpdate(*queries.toTypedArray())
    }
  }
}
