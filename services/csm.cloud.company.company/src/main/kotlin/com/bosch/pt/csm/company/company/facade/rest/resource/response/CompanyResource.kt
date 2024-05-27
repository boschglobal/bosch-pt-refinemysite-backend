/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.company.company.facade.rest.resource.dto.PostBoxAddressDto
import com.bosch.pt.csm.company.company.facade.rest.resource.dto.StreetAddressDto
import java.util.Date
import java.util.UUID

data class CompanyResource(
    override val id: UUID,
    override val version: Long,
    override val createdDate: Date,
    override val createdBy: ResourceReference,
    override val lastModifiedDate: Date,
    override val lastModifiedBy: ResourceReference,
    val name: String,
    val streetAddress: StreetAddressDto?,
    val postBoxAddress: PostBoxAddressDto?,
) :
    AbstractAuditableResource(
        id, version, createdDate, createdBy, lastModifiedDate, lastModifiedBy) {

  companion object {
    const val LINK_EMPLOYEES = "employees"
    const val LINK_CSMS = "constructionsitemanagers"
    const val LINK_FMS = "foremen"
    const val LINK_CAS = "administrator"
    const val LINK_CRS = "representative"
    const val LINK_DELETE = "delete"
  }
}
