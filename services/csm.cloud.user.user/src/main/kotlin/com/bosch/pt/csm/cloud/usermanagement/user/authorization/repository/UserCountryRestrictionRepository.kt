/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.authorization.repository

import com.bosch.pt.csm.cloud.usermanagement.user.authorization.model.UserCountryRestriction
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UserCountryRestrictionRepository : JpaRepository<UserCountryRestriction, Long> {

  @Query("select uca.countryCode from UserCountryRestriction uca where uca.userId = :userId")
  fun findAllCountriesByUserId(@Param("userId") userId: UUID): Set<IsoCountryCodeEnum>
}
