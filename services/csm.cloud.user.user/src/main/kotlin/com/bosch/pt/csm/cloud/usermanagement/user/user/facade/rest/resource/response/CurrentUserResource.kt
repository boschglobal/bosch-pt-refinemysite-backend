/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.usermanagement.application.security.SecurityContextHelper
import com.bosch.pt.csm.cloud.usermanagement.user.user.GenderEnum
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.datastructure.PhoneNumberDto
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import java.util.Locale
import java.util.UUID

class CurrentUserResource(
    val id: UUID,
    val version: Long,
    val gender: GenderEnum?,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val position: String?,
    val crafts: List<ResourceReference>,
    val phoneNumbers: List<PhoneNumberDto>,
    val admin: Boolean? = null,
    val registered: Boolean? = null,
    val eulaAccepted: Boolean,
    val locale: Locale?,
    val country: IsoCountryCodeEnum?
) : AbstractResource() {

  @ExcludeFromCodeCoverage
  constructor(
      user: User,
      crafts: List<ResourceReference>
  ) : this(
      user.getIdentifierUuid(),
      user.version,
      user.gender,
      user.firstName,
      user.lastName,
      user.email,
      user.position,
      crafts,
      user.phonenumbers
          .map { PhoneNumberDto(it.countryCode!!, it.phoneNumberType!!, it.callNumber!!) }
          .sortedWith(
              Comparator.comparing(PhoneNumberDto::phoneNumberType, Comparator.naturalOrder())
                  .thenComparing(PhoneNumberDto::phoneNumber, Comparator.naturalOrder())),
      if (SecurityContextHelper.getCurrentUser().admin) user.admin else null,
      if (SecurityContextHelper.getCurrentUser().admin) user.registered else null,
      user.eulaAcceptedDate != null,
      user.locale,
      user.country)

  companion object {
    const val LINK_CURRENT_USER_UPDATE = "update"
  }
}
