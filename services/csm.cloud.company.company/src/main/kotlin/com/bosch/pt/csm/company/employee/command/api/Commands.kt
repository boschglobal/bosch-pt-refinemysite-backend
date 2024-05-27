/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.command.api

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.company.company.CompanyId
import com.bosch.pt.csm.company.employee.EmployeeId
import com.bosch.pt.csm.company.employee.shared.model.EmployeeRoleEnum

@Suppress("UnnecessaryAbstractClass")
abstract class EmployeeCommand(open val identifier: EmployeeId)

data class CreateEmployeeCommand(
    val identifier: EmployeeId? = null,
    val userRef: UserId,
    val companyRef: CompanyId,
    val roles: List<EmployeeRoleEnum>
)

data class UpdateEmployeeCommand(
    override val identifier: EmployeeId,
    val version: Long,
    val roles: List<EmployeeRoleEnum>
) : EmployeeCommand(identifier)

data class DeleteEmployeeCommand(override val identifier: EmployeeId) : EmployeeCommand(identifier)
