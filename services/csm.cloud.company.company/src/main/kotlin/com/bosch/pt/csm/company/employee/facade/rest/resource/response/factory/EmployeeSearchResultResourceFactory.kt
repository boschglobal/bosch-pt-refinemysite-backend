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
import com.bosch.pt.csm.company.employee.facade.rest.EmployeeController.Companion.EMPLOYEE_SEARCH_ENDPOINT_PATH
import com.bosch.pt.csm.company.employee.facade.rest.resource.response.EmployeeSearchResultItemResource
import com.bosch.pt.csm.user.user.model.dto.UserWithEmployeeCompanySearchResultDto
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class EmployeeSearchResultResourceFactory(
    private val employeeSearchResultResourceFactoryHelper:
    EmployeeSearchResultResourceFactoryHelper,
    private val linkFactory: CustomLinkBuilderFactory
) {

  @PageLinks
  fun build(
      employees: Page<UserWithEmployeeCompanySearchResultDto>
  ): ListResponseResource<EmployeeSearchResultItemResource> {
    val employeeResources =
        employeeSearchResultResourceFactoryHelper.buildEmployeeSearchResultResource(
            employees.content)

    val employeePageResource =
        ListResponseResource(
            employeeResources,
            employees.number,
            employees.size,
            employees.totalPages,
            employees.totalElements)

    employeePageResource.add(linkFactory.linkTo(EMPLOYEE_SEARCH_ENDPOINT_PATH).withSelfRel())

    return employeePageResource
  }
}
