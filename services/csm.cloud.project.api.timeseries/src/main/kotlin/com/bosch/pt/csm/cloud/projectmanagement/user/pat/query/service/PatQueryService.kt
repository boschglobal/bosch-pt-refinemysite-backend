/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.service

import com.bosch.pt.csm.cloud.projectmanagement.application.security.NoPreAuthorize
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.domain.PatId
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatProjection
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.repository.PatProjectionRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class PatQueryService(private val repository: PatProjectionRepository) {

  @Cacheable(cacheNames = ["pat-by-identifier"])
  @NoPreAuthorize
  fun findByIdentifier(patId: PatId): PatProjection? = repository.findOneByIdentifier(patId)
}
