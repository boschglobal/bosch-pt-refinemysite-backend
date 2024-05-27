/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.employee.model

import com.bosch.pt.iot.smartsite.dataimport.common.model.ImportObject
import com.bosch.pt.iot.smartsite.dataimport.employee.api.resource.EmployeeRoleEnum

class Employee(
    override val id: String,
    val version: Long,
    val userId: String,
    val roles: List<EmployeeRoleEnum>? = null,
    val companyId: String
) : ImportObject
