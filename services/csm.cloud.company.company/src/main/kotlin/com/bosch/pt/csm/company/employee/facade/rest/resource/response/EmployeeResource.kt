/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.employee.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.company.employee.shared.model.EmployeeRoleEnum
import java.util.Date
import java.util.UUID

data class EmployeeResource(
    override val id: UUID,
    override val version: Long,
    override val createdDate: Date,
    override val createdBy: ResourceReference,
    override val lastModifiedDate: Date,
    override val lastModifiedBy: ResourceReference,
    val user: ResourceReference,
    val company: ResourceReference,
    val roles: List<EmployeeRoleEnum>
) :
    AbstractAuditableResource(
        id, version, createdDate, createdBy, lastModifiedDate, lastModifiedBy) {

  companion object {
    const val LINK_DELETE_EMPLOYEE = "delete"
  }
}
