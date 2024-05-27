/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.GenderEnum
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.datastructure.PhoneNumberDto
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import java.util.Locale

class UserResource(
    user: User,
    crafts: List<ResourceReference>,
    createByName: String,
    lastModifiedByName: String
) : AbstractAuditableResource(user, createByName, lastModifiedByName) {

  val gender: GenderEnum?
  val firstName: String?
  val lastName: String?
  val email: String?
  val position: String?
  val phoneNumbers: List<PhoneNumberDto>
  val crafts: List<ResourceReference>
  var admin: Boolean
  var registered: Boolean
  val eulaAccepted: Boolean
  var locked: Boolean
  var locale: Locale?
  var country: IsoCountryCodeEnum?

  companion object {
    const val LINK_DELETE = "delete"
    const val LINK_LOCK = "lock"
    const val LINK_UNLOCK = "unlock"
    const val LINK_SET_ADMIN = "setAdmin"
    const val LINK_UNSET_ADMIN = "unsetAdmin"
  }

  init {
    gender = user.gender
    firstName = user.firstName
    lastName = user.lastName
    email = user.email
    position = user.position
    phoneNumbers =
        user.phonenumbers
            .map { PhoneNumberDto(it.countryCode!!, it.phoneNumberType!!, it.callNumber!!) }
            .sortedWith(
                Comparator.comparing(PhoneNumberDto::phoneNumberType, Comparator.naturalOrder())
                    .thenComparing(PhoneNumberDto::phoneNumber, Comparator.naturalOrder()))
    this.crafts = crafts
    admin = user.admin
    registered = user.registered
    locked = user.locked
    eulaAccepted = user.eulaAcceptedDate != null
    locale = user.locale
    country = user.country
  }
}
