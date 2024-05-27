/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.employee.api.resource.response

import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.AuditableResource
import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response.UserReference
import com.bosch.pt.iot.smartsite.dataimport.employee.api.resource.EmployeeRoleEnum
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID

@JsonIgnoreProperties("_links")
class EmployeeResource(
    val id: UUID,
    val user: UserReference,
    val roles: List<EmployeeRoleEnum>? = null,
    val company: ResourceReference
) : AuditableResource()
