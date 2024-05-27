/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.employee.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.csm.common.facade.rest.resource.response.ResourceReferenceWithEmail
import com.bosch.pt.csm.common.model.ResourceReferenceAssembler.referTo
import com.bosch.pt.csm.company.employee.facade.rest.EmployeeController.Companion.EMPLOYEE_BY_EMPLOYEE_ID_ENDPOINT_PATH
import com.bosch.pt.csm.company.employee.facade.rest.EmployeeController.Companion.PATH_VARIABLE_EMPLOYEE_ID
import com.bosch.pt.csm.company.employee.facade.rest.resource.response.EmployeeResource
import com.bosch.pt.csm.company.employee.facade.rest.resource.response.EmployeeResource.Companion.LINK_DELETE_EMPLOYEE
import com.bosch.pt.csm.company.employee.shared.model.Employee
import com.bosch.pt.csm.user.user.query.UserProjection
import com.bosch.pt.csm.user.user.query.UserQueryService
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class EmployeeResourceFactoryHelper(
    private val userQueryService: UserQueryService,
    private val linkFactory: CustomLinkBuilderFactory,
    messageSource: MessageSource
) : AbstractResourceFactoryHelper(messageSource) {

  fun buildEmployeeResources(employees: List<Employee>): List<EmployeeResource> =
      if (employees.isEmpty()) {
        emptyList()
      } else {
        val auditUser =
            employees
                .map {
                  setOf(it.createdBy.get(), it.lastModifiedBy.get(), checkNotNull(it.userRef))
                }
                .flatten()
                .toSet()
                .let { userQueryService.findAll(it) }
                .associateBy { it.id }

        employees.map {
          build(
              it,
              auditUser[it.createdBy.get()],
              auditUser[it.lastModifiedBy.get()],
              auditUser[checkNotNull(it.userRef)])
        }
      }

  private fun build(
      employee: Employee,
      createdBy: UserProjection?,
      lastModifiedBy: UserProjection?,
      employeeUserRef: UserProjection?
  ): EmployeeResource =
      EmployeeResource(
              id = employee.getIdentifierUuid(),
              version = employee.version,
              createdDate = employee.createdDate.get().toDate(),
              createdBy = referTo(createdBy, getDeletedUserReference()),
              lastModifiedDate = employee.lastModifiedDate.get().toDate(),
              lastModifiedBy = referTo(lastModifiedBy, getDeletedUserReference()),
              user =
                  employeeUserRef?.let {
                    ResourceReferenceWithEmail(it.id.toUuid(), it.getDisplayName(), it.email)
                  }
                      ?: getDeletedUserReference().get(),
              company = ResourceReference.from(checkNotNull(employee.company)),
              roles = employee.roles?.toList() ?: emptyList())
          .apply {
            this.add(
                linkFactory
                    .linkTo(EMPLOYEE_BY_EMPLOYEE_ID_ENDPOINT_PATH)
                    .withParameters(
                        mapOf(PATH_VARIABLE_EMPLOYEE_ID to employee.getIdentifierUuid()))
                    .withSelfRel())

            this.add(
                linkFactory
                    .linkTo(EMPLOYEE_BY_EMPLOYEE_ID_ENDPOINT_PATH)
                    .withParameters(
                        mapOf(PATH_VARIABLE_EMPLOYEE_ID to employee.getIdentifierUuid()))
                    .withRel(LINK_DELETE_EMPLOYEE))
          }
}
