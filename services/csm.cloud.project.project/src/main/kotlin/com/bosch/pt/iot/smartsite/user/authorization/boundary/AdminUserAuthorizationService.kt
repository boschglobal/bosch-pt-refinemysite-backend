/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.authorization.boundary

import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.user.authorization.repository.UserCountryRestrictionRepository
import org.springframework.stereotype.Service

@Service
class AdminUserAuthorizationService(
    private val userCountryRestrictionRepository: UserCountryRestrictionRepository
) {

  fun getRestrictedCountries() =
      SecurityContextHelper.getInstance().getCurrentUser().identifier!!.let {
        userCountryRestrictionRepository.findAllCountriesByUserId(it)
      }
}
