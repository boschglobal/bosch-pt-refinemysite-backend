/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.user.query.service

import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.model.UserProjection
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.repository.UserProjectionRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class UserQueryService(private val repository: UserProjectionRepository) {

  @Cacheable(cacheNames = ["users-by-identifiers"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByIdentifiers(userIds: List<UserId>): List<UserProjection> =
      repository.findAllByIdentifierIn(userIds)
}
