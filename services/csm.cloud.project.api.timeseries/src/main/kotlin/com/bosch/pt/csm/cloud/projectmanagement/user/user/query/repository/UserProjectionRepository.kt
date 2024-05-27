/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.user.query.repository

import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.model.UserProjection
import org.springframework.data.mongodb.repository.MongoRepository

interface UserProjectionRepository : MongoRepository<UserProjection, UserId> {

  fun findOneByIdpIdentifier(idpIdentifier: String): UserProjection?

  fun findOneByIdentifier(id: UserId): UserProjection?

  fun findAllByIdentifierIn(userIds: List<UserId>): List<UserProjection>
}
