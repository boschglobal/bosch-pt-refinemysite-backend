/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.datastructure

import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.PhoneNumber
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.PhoneNumber.Companion.PATTERN_COUNTRY_CODE
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.PhoneNumber.Companion.PATTERN_NUMBER
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.PhoneNumberType
import jakarta.validation.constraints.Pattern

data class PhoneNumberDto(
    val countryCode: @Pattern(regexp = PATTERN_COUNTRY_CODE) String,
    val phoneNumberType: PhoneNumberType,
    val phoneNumber: @Pattern(regexp = PATTERN_NUMBER) String
)

fun Set<PhoneNumberDto>?.toPhoneNumbers(): MutableSet<PhoneNumber> =
    if (this.isNullOrEmpty()) {
      mutableSetOf()
    } else
        this.map { PhoneNumber(it.phoneNumberType, it.countryCode, it.phoneNumber) }.toMutableSet()
