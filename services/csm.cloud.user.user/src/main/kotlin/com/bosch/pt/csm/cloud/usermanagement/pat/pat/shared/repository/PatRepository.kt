/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.repository

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.PatId
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.Pat
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository

interface PatRepository : JpaRepository<Pat, Long> {

  fun findByIdentifier(identifier: PatId): Pat?

  fun findByImpersonatedUser(userId: UserId, sort: Sort): List<Pat>

  fun deleteByIdentifier(patId: PatId)
}
