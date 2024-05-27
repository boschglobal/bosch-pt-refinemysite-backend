/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import java.util.Locale

data class UserResource(
    val id: UserId,
    val version: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val position: String? = null,
    val locale: Locale? = null,
    val country: IsoCountryCodeEnum? = null,
    val phoneNumbers: List<UserPhoneNumberDto>,
    val eventTimestamp: Long
)

data class UserPhoneNumberDto(
    val countryCode: String,
    val phoneNumberType: String,
    val callNumber: String
)
