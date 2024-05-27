/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.employee.facade.rest.resource.request

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.company.employee.shared.model.EmployeeRoleEnum
import jakarta.validation.constraints.Size

class SaveEmployeeResource(
    var userId: UserId,
    @field:Size(min = 1) var roles: List<EmployeeRoleEnum>
)
