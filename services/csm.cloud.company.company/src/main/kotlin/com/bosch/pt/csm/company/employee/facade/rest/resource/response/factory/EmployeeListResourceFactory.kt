/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.employee.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.factory.PageLinks
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ListResponseResource
import com.bosch.pt.csm.company.company.shared.model.Company
import com.bosch.pt.csm.company.employee.facade.rest.EmployeeController.Companion.EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH
import com.bosch.pt.csm.company.employee.facade.rest.EmployeeController.Companion.PATH_VARIABLE_COMPANY_ID
import com.bosch.pt.csm.company.employee.facade.rest.resource.response.EmployeeResource
import com.bosch.pt.csm.company.employee.shared.model.Employee
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class EmployeeListResourceFactory(
    private val employeeResourceFactoryHelper: EmployeeResourceFactoryHelper,
    private val linkFactory: CustomLinkBuilderFactory,
) {

  @PageLinks
  fun buildListItems(
      employees: Page<Employee>,
      company: Company
  ): ListResponseResource<EmployeeResource> {
    val employeeResources = employeeResourceFactoryHelper.buildEmployeeResources(employees.content)
    val employeePageResource =
        ListResponseResource(
            employeeResources,
            employees.number,
            employees.size,
            employees.totalPages,
            employees.totalElements)

    employeePageResource.add(
        linkFactory
            .linkTo(EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH)
            .withParameters(mapOf(PATH_VARIABLE_COMPANY_ID to company.identifier))
            .withSelfRel())

    return employeePageResource
  }
}
