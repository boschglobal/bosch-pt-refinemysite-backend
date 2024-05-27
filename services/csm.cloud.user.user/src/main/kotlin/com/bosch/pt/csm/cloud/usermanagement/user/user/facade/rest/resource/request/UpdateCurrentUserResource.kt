/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.request

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.facade.rest.resource.validation.StringEnumeration
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.CraftId
import com.bosch.pt.csm.cloud.usermanagement.user.user.GenderEnum
import com.bosch.pt.csm.cloud.usermanagement.user.user.GenderEnum.Companion.ENUM_VALUES
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.PhoneNumberCommandDto
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.UpdateUserCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.datastructure.PhoneNumberDto
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User.Companion.MAX_FIRST_NAME_LENGTH
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User.Companion.MAX_LAST_NAME_LENGTH
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User.Companion.MAX_PHONE_NUMBERS
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User.Companion.MAX_POSITION_LENGTH
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.util.Locale

data class UpdateCurrentUserResource(
    @StringEnumeration(enumClass = GenderEnum::class, enumValues = ENUM_VALUES)
    val gender: GenderEnum,
    @field:Size(max = MAX_FIRST_NAME_LENGTH) val firstName: String,
    @field:Size(max = MAX_LAST_NAME_LENGTH) val lastName: String,
    @field:Size(max = MAX_POSITION_LENGTH) val position: String? = null,
    val craftIds: List<CraftId> = ArrayList(),
    @Valid
    @field:Size(max = MAX_PHONE_NUMBERS)
    val phoneNumbers: MutableSet<PhoneNumberDto> = HashSet(),
    val locale: Locale,
    @StringEnumeration(enumClass = IsoCountryCodeEnum::class) val country: IsoCountryCodeEnum
) {
  fun toCommand(identifier: UserId, version: Long) =
      UpdateUserCommand(
          identifier = identifier,
          version = version,
          gender = gender,
          firstName = firstName,
          lastName = lastName,
          position = position,
          crafts = craftIds,
          phoneNumbers =
              phoneNumbers
                  .map {
                    PhoneNumberCommandDto(
                        countryCode = it.countryCode,
                        callNumber = it.phoneNumber,
                        phoneNumberType = it.phoneNumberType,
                    )
                  }
                  .toSet(),
          locale = locale,
          country = country)
}
