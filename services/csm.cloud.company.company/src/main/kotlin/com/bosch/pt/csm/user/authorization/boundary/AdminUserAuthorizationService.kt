/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.user.authorization.boundary

import com.bosch.pt.csm.application.security.NoPreAuthorize
import com.bosch.pt.csm.application.security.SecurityContextHelper
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.user.authorization.repository.UserCountryRestrictionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminUserAuthorizationService(
    private val userCountryRestrictionRepository: UserCountryRestrictionRepository
) {

  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun getRestrictedCountries() =
      SecurityContextHelper.getCurrentUser().id.let {
        userCountryRestrictionRepository.findAllCountriesByUserId(it.toUuid())
      }

  @NoPreAuthorize
  @Transactional(readOnly = true)
  fun authorizedForCountry(country: IsoCountryCodeEnum?): Boolean =
      getRestrictedCountries().let { it.isEmpty() || country in it }
}

fun Set<IsoCountryCodeEnum>.toSetOfAlternativeCountryNames() =
    this.map { code -> code.alternativeCountryName }.toSet()
