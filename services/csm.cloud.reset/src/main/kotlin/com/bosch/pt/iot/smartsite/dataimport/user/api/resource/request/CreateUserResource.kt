/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.user.api.resource.request

import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.iot.smartsite.dataimport.user.model.PhoneNumber
import java.util.Locale
import java.util.UUID

class CreateUserResource(
    var gender: String? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var email: String? = null,
    var position: String? = null,
    var roles: Set<String>? = null,
    var phoneNumbers: Set<PhoneNumber>? = null,
    var craftIds: Set<UUID>? = null,
    var eulaAccepted: Boolean? = null,
    var locale: Locale? = null,
    var country: IsoCountryCodeEnum? = null
)
