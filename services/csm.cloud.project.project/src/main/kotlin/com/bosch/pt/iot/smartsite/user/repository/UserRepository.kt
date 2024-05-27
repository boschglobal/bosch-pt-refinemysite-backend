/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.repository

import com.bosch.pt.iot.smartsite.common.repository.ReplicatedEntityRepository
import com.bosch.pt.iot.smartsite.user.model.User
import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph

interface UserRepository : ReplicatedEntityRepository<User, Long>, UserRepositoryExtension {

  fun findAllByIdentifierIn(identifiers: Set<UUID>): List<User>

  fun findOneByIdentifier(identifier: UUID): User?

  fun findOneByCiamUserIdentifier(ciamUserIdentifier: String): User?

  @EntityGraph(attributePaths = ["profilePicture"])
  fun findOneWithPictureByCiamUserIdentifier(ciamUserIdentifier: String): User?

  fun findOneByEmail(email: String): User?
}
