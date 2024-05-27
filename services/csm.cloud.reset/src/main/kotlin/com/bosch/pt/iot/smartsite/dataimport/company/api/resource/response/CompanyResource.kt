/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.company.api.resource.response

import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.AuditableResource
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID

@JsonIgnoreProperties("_links")
class CompanyResource(
    val id: UUID,
    val name: String,
    val streetAddress: StreetAddressResource? = null,
    val postBoxAddress: PostOfficeBoxAddress? = null
) : AuditableResource()
