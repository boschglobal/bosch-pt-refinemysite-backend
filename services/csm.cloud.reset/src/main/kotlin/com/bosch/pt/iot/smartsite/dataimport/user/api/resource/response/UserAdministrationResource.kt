/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.user.api.resource.response

import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.AuditableResource
import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.dataimport.user.api.resource.response.dto.PhoneNumber
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Locale
import java.util.UUID

@JsonIgnoreProperties("_embedded", "_links")
class UserAdministrationResource(
    @JsonProperty("id") var identifier: UUID,
    var gender: String? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var email: String? = null,
    var position: String? = null,
    var company: ResourceReference? = null,
    var admin: Boolean = false,
    var registered: Boolean = false,
    var locked: Boolean = false,
    var phoneNumbers: List<PhoneNumber>? = null,
    var crafts: List<ResourceReference>? = null,
    var picture: ProfilePictureResource? = null,
    var eulaAccepted: Boolean = false,
    var locale: Locale? = null,
    var country: IsoCountryCodeEnum? = null
) : AuditableResource()
