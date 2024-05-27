/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.user.api.resource.response

import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.AuditableResource
import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.dataimport.user.model.PhoneNumber
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Locale
import java.util.UUID

@JsonIgnoreProperties("_embedded", "_links")
class UserResource(
    @JsonProperty("id") var identifier: UUID,
    var registered: Boolean = false,
    var userId: String? = null,
    var gender: String? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var email: String? = null,
    var position: String? = null,
    var admin: Boolean = false,
    var roles: List<String>? = null,
    var phoneNumbers: List<PhoneNumber>? = null,
    var crafts: List<ResourceReference>? = null,
    var picture: ProfilePictureResource? = null,
    var eulaAccepted: Boolean = false,
    var locale: Locale? = null,
    var country: IsoCountryCodeEnum? = null
) : AuditableResource()
