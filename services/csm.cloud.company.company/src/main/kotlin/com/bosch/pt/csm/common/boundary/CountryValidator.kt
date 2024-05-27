/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common.boundary

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.Companion.fromAlternativeCountryNameExists
import com.bosch.pt.csm.common.i18n.Key.COMPANY_VALIDATION_ERROR_INVALID_COUNTRY_NAME
import org.springframework.stereotype.Component

@Component
class CountryValidator {

  fun validateCountryName(country: String?) =
      if (country == null || !fromAlternativeCountryNameExists(country)) {
        throw PreconditionViolationException(COMPANY_VALIDATION_ERROR_INVALID_COUNTRY_NAME)
      } else Unit
}
