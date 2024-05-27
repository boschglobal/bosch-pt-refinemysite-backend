/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.authorization.boundary

import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.usermanagement.application.security.NoPreAuthorize
import com.bosch.pt.csm.cloud.usermanagement.application.security.SecurityContextHelper.getCurrentUser
import com.bosch.pt.csm.cloud.usermanagement.user.authorization.repository.UserCountryRestrictionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminUserAuthorizationService(
    private val userCountryRestrictionRepository: UserCountryRestrictionRepository
) {

  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun getRestrictedCountries() =
      getCurrentUser().getIdentifierUuid().let {
        userCountryRestrictionRepository.findAllCountriesByUserId(it)
      }

  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun authorizedForCountry(country: IsoCountryCodeEnum?) =
      getRestrictedCountries().let { it.isEmpty() || it.contains(country) }
}
