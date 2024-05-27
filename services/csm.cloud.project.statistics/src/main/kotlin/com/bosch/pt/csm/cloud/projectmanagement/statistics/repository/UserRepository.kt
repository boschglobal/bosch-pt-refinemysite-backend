/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.repository

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.User
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {

  fun findOneByUserId(userId: String): User?

  fun findOneByIdentifier(identifier: UUID): User?

  fun deleteByIdentifier(identifier: UUID): Long
}
