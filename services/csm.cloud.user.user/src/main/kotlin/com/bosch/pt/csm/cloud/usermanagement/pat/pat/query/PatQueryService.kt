/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.query

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.usermanagement.application.security.NoPreAuthorize
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.PatId
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.Pat
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.repository.PatRepository
import org.springframework.data.domain.Sort
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class PatQueryService(private val patRepository: PatRepository) {

  @NoPreAuthorize
  @PostAuthorize("returnObject?.impersonatedUser == principal.identifier")
  fun findByPatId(patId: PatId): Pat? = patRepository.findByIdentifier(patId)

  @PreAuthorize("#userId == principal.identifier")
  fun findByImpersonatedUser(userId: UserId) =
      patRepository.findByImpersonatedUser(userId, Sort.by("issuedAt"))
}
