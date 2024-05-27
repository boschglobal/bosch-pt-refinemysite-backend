/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.user.user.query

import com.bosch.pt.csm.cloud.common.api.UserId
import org.springframework.data.jpa.repository.JpaRepository

interface UserProjectionRepository : JpaRepository<UserProjection, UserId> {

  fun findOneById(id: UserId): UserProjection?

  fun findOneByCiamUserIdentifier(ciamUserIdentifier: String): UserProjection?

  fun findAllByIdIn(id: Set<UserId>): Set<UserProjection>
}
