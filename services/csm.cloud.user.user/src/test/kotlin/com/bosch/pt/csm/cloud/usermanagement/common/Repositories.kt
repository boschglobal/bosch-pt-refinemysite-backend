/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.common

import com.bosch.pt.csm.cloud.usermanagement.announcement.repository.AnnouncementPermissionRepository
import com.bosch.pt.csm.cloud.usermanagement.announcement.repository.AnnouncementRepository
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.repository.CraftRepository
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.repository.PatRepository
import com.bosch.pt.csm.cloud.usermanagement.user.authorization.repository.UserCountryRestrictionRepository
import com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.repository.ProfilePictureRepository
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.repository.UserRepository
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
    val userRepository: UserRepository,
    val profilePictureRepository: ProfilePictureRepository,
    val patRepository: PatRepository,
    val craftRepository: CraftRepository,
    val announcementRepository: AnnouncementRepository,
    val announcementPermissionRepository: AnnouncementPermissionRepository,
    val userCountryRestrictionRepository: UserCountryRestrictionRepository,
    val dataSource: DataSource
) {

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
