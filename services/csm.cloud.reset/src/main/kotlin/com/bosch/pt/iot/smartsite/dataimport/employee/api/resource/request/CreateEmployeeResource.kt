/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.employee.api.resource.request

import com.bosch.pt.iot.smartsite.dataimport.employee.api.resource.EmployeeRoleEnum
import java.util.UUID

class CreateEmployeeResource(val userId: UUID? = null, val roles: List<EmployeeRoleEnum>? = null)
