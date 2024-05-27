/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.employee.facade.rest.resource.response.factory

import com.bosch.pt.csm.company.employee.facade.rest.resource.response.EmployeeResource
import com.bosch.pt.csm.company.employee.shared.model.Employee
import org.springframework.stereotype.Component

@Component
class EmployeeResourceFactory(
    private val employeeResourceFactoryHelper: EmployeeResourceFactoryHelper
) {

  fun build(employee: Employee): EmployeeResource =
      employeeResourceFactoryHelper.buildEmployeeResources(listOf(employee)).first()
}
