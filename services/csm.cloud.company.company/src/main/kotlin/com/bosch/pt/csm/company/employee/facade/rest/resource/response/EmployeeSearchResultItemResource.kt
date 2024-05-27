/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.employee.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.company.employee.facade.rest.resource.response.dto.UserDto

/**
 * This is an entry of the list showing a combined view on employees and users. It is used to get a
 * list of all employees including those users that are not yet assigned to a company. Therefore, it
 * contains resource references to the user, the employee and the company and contains links related
 * the the user and the employee.
 */
class EmployeeSearchResultItemResource(
    val user: UserDto,
    val company: ResourceReference?,
    val employee: ResourceReference?
) : AbstractResource() {

  companion object {
    const val LINK_DELETE_USER = "deleteUser"
    const val LINK_CREATE_EMPLOYEE = "createEmployee"
    const val LINK_EDIT_EMPLOYEE = "editEmployee"
    const val LINK_SET_ADMIN = "setAdmin"
    const val LINK_UNSET_ADMIN = "unsetAdmin"
    const val LINK_LOCK = "lock"
    const val LINK_UNLOCK = "unlock"
  }
}
