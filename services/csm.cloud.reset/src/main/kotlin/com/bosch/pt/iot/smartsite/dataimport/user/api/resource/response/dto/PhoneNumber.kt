/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.user.api.resource.response.dto

import com.bosch.pt.iot.smartsite.dataimport.user.model.PhoneNumberType

class PhoneNumber(
    val countryCode: String? = null,
    val phoneNumberType: PhoneNumberType? = null,
    val phoneNumber: String? = null
)
