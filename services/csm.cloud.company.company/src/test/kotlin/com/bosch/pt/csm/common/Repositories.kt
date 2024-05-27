/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common

import com.bosch.pt.csm.company.company.shared.repository.CompanyRepository
import com.bosch.pt.csm.company.employee.query.employableuser.EmployableUserCompanyNameRepository
import com.bosch.pt.csm.company.employee.query.employableuser.EmployableUserProjectionRepository
import com.bosch.pt.csm.company.employee.shared.repository.EmployeeRepository
import com.bosch.pt.csm.user.authorization.repository.UserCountryRestrictionRepository
import com.bosch.pt.csm.user.user.query.UserProjectionRepository
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
    val employeeRepository: EmployeeRepository,
    val companyRepository: CompanyRepository,
    val userCountryRestrictionRepository: UserCountryRestrictionRepository,
    val userProjectionRepository: UserProjectionRepository,
    val employableUserProjectionRepository: EmployableUserProjectionRepository,
    val employableUserProjectionCompanyNameRepository: EmployableUserCompanyNameRepository,
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
