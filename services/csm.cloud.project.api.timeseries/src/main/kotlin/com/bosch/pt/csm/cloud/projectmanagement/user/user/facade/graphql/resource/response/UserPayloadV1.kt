/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.graphql.resource.response

import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID

data class UserPayloadV1(
    val id: UUID,
    val version: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val position: String? = null,
    val locale: Locale? = null,
    val country: IsoCountryCodeEnum? = null,
    val phoneNumbers: List<UserPhoneNumberPayloadV1>,
    val eventDate: LocalDateTime
)

data class UserPhoneNumberPayloadV1(
    val countryCode: String,
    val phoneNumberType: String,
    val callNumber: String
)
