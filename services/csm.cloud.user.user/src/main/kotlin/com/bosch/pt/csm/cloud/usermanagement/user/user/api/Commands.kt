/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.user.api

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.CraftId
import com.bosch.pt.csm.cloud.usermanagement.user.user.GenderEnum
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.PhoneNumberType
import java.util.Locale

data class RegisterUserCommand(
    val externalUserId: String,
    val eulaAccepted: Boolean,
    val gender: GenderEnum,
    val firstName: String,
    val lastName: String,
    val email: String,
    val position: String? = null,
    val crafts: List<CraftId>,
    val phoneNumbers: Set<PhoneNumberCommandDto> = emptySet(),
    val locale: Locale,
    val country: IsoCountryCodeEnum
)

data class UpdateEmailCommand(
    val identifier: UserId,
    val version: Long,
    val email: String,
)

data class UpdateUserCommand(
    val identifier: UserId,
    val version: Long,
    val gender: GenderEnum?,
    val firstName: String?,
    val lastName: String?,
    val position: String? = null,
    val crafts: List<CraftId>,
    val phoneNumbers: Set<PhoneNumberCommandDto> = emptySet(),
    val locale: Locale,
    val country: IsoCountryCodeEnum
)

data class PhoneNumberCommandDto(
    var countryCode: String?,
    var callNumber: String?,
    var phoneNumberType: PhoneNumberType?
)

data class LockUserCommand(val identifier: UserId)

data class UnlockUserCommand(val identifier: UserId)

data class GrantAdminPrivilegeCommand(val identifier: UserId)

data class RevokeAdminPrivilegeCommand(val identifier: UserId)

data class DeleteUserCommand(val identifier: UserId)

data class DeleteUserAfterSkidDeletionCommand(val identifier: UserId)
