/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.company.api.resource.response

import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.AuditableResource
import java.util.UUID

class StreetAddressResource(
    val id: UUID? = null,
    val city: String? = null,
    val zipCode: String? = null,
    val area: String? = null,
    val country: String? = null,
    val street: String? = null,
    val houseNumber: String? = null
) : AuditableResource()
