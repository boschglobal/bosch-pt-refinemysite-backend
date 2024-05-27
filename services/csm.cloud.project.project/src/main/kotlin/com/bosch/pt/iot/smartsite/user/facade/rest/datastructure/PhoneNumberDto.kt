/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.facade.rest.datastructure

import com.bosch.pt.iot.smartsite.user.model.PhoneNumber.Companion.PATTERN_COUNTRY_CODE
import com.bosch.pt.iot.smartsite.user.model.PhoneNumber.Companion.PATTERN_NUMBER
import com.bosch.pt.iot.smartsite.user.model.PhoneNumberType
import jakarta.validation.constraints.Pattern

class PhoneNumberDto(
    val phoneNumberType: PhoneNumberType,
    @field:Pattern(regexp = PATTERN_COUNTRY_CODE) val countryCode: String,
    @field:Pattern(regexp = PATTERN_NUMBER) val phoneNumber: String
)
