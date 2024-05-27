/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.employee.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.company.employee.facade.rest.EmployeeController.Companion.EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH
import com.bosch.pt.csm.company.employee.facade.rest.EmployeeController.Companion.EMPLOYEE_BY_EMPLOYEE_ID_ENDPOINT_PATH
import com.bosch.pt.csm.company.employee.facade.rest.EmployeeController.Companion.PATH_VARIABLE_COMPANY_ID
import com.bosch.pt.csm.company.employee.facade.rest.EmployeeController.Companion.PATH_VARIABLE_EMPLOYEE_ID
import com.bosch.pt.csm.company.employee.facade.rest.resource.response.EmployeeSearchResultItemResource
import com.bosch.pt.csm.company.employee.facade.rest.resource.response.EmployeeSearchResultItemResource.Companion.LINK_CREATE_EMPLOYEE
import com.bosch.pt.csm.company.employee.facade.rest.resource.response.EmployeeSearchResultItemResource.Companion.LINK_DELETE_USER
import com.bosch.pt.csm.company.employee.facade.rest.resource.response.EmployeeSearchResultItemResource.Companion.LINK_EDIT_EMPLOYEE
import com.bosch.pt.csm.company.employee.facade.rest.resource.response.EmployeeSearchResultItemResource.Companion.LINK_LOCK
import com.bosch.pt.csm.company.employee.facade.rest.resource.response.EmployeeSearchResultItemResource.Companion.LINK_SET_ADMIN
import com.bosch.pt.csm.company.employee.facade.rest.resource.response.EmployeeSearchResultItemResource.Companion.LINK_UNLOCK
import com.bosch.pt.csm.company.employee.facade.rest.resource.response.EmployeeSearchResultItemResource.Companion.LINK_UNSET_ADMIN
import com.bosch.pt.csm.company.employee.facade.rest.resource.response.dto.UserDto
import com.bosch.pt.csm.user.user.boundary.UserPreconditionService
import com.bosch.pt.csm.user.user.facade.resource.factory.UserUriBuilder
import com.bosch.pt.csm.user.user.model.dto.UserWithEmployeeCompanySearchResultDto
import java.util.UUID.randomUUID
import org.springframework.hateoas.Link
import org.springframework.stereotype.Component

@Component
class EmployeeSearchResultResourceFactoryHelper(
    private val userUriBuilder: UserUriBuilder,
    private val userPreconditionService: UserPreconditionService,
    private val linkFactory: CustomLinkBuilderFactory
) {

  fun buildEmployeeSearchResultResource(
      usersWithEmployee: List<UserWithEmployeeCompanySearchResultDto>
  ): List<EmployeeSearchResultItemResource> =
      if (usersWithEmployee.isEmpty()) {
        emptyList()
      } else usersWithEmployee.map { buildListItem(it) }

  private fun buildListItem(
      userWithEmployee: UserWithEmployeeCompanySearchResultDto
  ): EmployeeSearchResultItemResource {

    val resource =
        EmployeeSearchResultItemResource(
            buildUserReference(userWithEmployee),
            buildCompanyReference(userWithEmployee),
            buildEmployeeReference(userWithEmployee))

    if (userWithEmployee.employeeIdentifier != null) {
      resource.add(
          linkFactory
              .linkTo(EMPLOYEE_BY_EMPLOYEE_ID_ENDPOINT_PATH)
              .withParameters(
                  mapOf(PATH_VARIABLE_EMPLOYEE_ID to userWithEmployee.employeeIdentifier))
              .withRel(LINK_EDIT_EMPLOYEE))
    } else {
      // This link is fake. we use the link name of the UI do control, that a user can be assigned
      // to a company. Since we do not know to which company, we cannot generate a proper link
      // target here.
      resource.add(
          linkFactory
              .linkTo(EMPLOYEES_BY_COMPANY_ID_ENDPOINT_PATH)
              .withParameters(mapOf(PATH_VARIABLE_COMPANY_ID to randomUUID()))
              .withRel(LINK_CREATE_EMPLOYEE))
    }

    if (userPreconditionService.isDeleteUserPossible(userWithEmployee.userIdentifier?.asUserId())) {
      resource.add(
          Link.of(
              userUriBuilder.buildUserUri(userWithEmployee.userIdentifier).toString(),
              LINK_DELETE_USER))
    }

    val linkUserRoleName = if (userWithEmployee.admin) LINK_UNSET_ADMIN else LINK_SET_ADMIN
    resource.add(
        Link.of(
            userUriBuilder.buildUserRoleUri(userWithEmployee.userIdentifier).toString(),
            linkUserRoleName))

    val linkLockName = if (userWithEmployee.locked) LINK_UNLOCK else LINK_LOCK
    resource.add(
        Link.of(
            userUriBuilder.buildUserLockUri(userWithEmployee.userIdentifier).toString(),
            linkLockName))

    return resource
  }

  private fun buildUserReference(
      userWithEmployee: UserWithEmployeeCompanySearchResultDto
  ): UserDto =
      UserDto(
          userWithEmployee.userIdentifier!!,
          userWithEmployee.getDisplayName(),
          userWithEmployee.email,
          userWithEmployee.gender!!,
          userWithEmployee.userCreatedDate!!,
          userWithEmployee.admin,
          userWithEmployee.locked)

  private fun buildCompanyReference(
      employee: UserWithEmployeeCompanySearchResultDto
  ): ResourceReference? =
      if (employee.companyIdentifier != null)
          ResourceReference(employee.companyIdentifier.toUuid(), employee.companyName.orEmpty())
      else null

  private fun buildEmployeeReference(
      employee: UserWithEmployeeCompanySearchResultDto
  ): ResourceReference? =
      if (employee.employeeIdentifier != null) ResourceReference.from(employee) else null
}
