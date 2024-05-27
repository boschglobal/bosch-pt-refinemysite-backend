/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.user.user.query

import com.bosch.pt.csm.application.security.NoPreAuthorize
import com.bosch.pt.csm.cloud.common.api.UserId
import datadog.trace.api.Trace
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserQueryService(private val repository: UserProjectionRepository) {

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun findOne(identifier: UserId): UserProjection? = repository.findOneById(identifier)

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun findAll(ids: Set<UserId>) = repository.findAllByIdIn(ids)

  @Trace
  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun findOneByCiamUserId(userId: String): UserProjection? =
      repository.findOneByCiamUserIdentifier(userId)
}
