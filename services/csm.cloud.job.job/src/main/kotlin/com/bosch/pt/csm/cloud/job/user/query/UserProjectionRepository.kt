/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.user.query

import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import org.springframework.data.mongodb.repository.MongoRepository

interface UserProjectionRepository : MongoRepository<UserProjection, UserIdentifier> {
  fun findByExternalUserIdentifier(externalUserIdentifier: ExternalUserIdentifier): UserProjection?
}
