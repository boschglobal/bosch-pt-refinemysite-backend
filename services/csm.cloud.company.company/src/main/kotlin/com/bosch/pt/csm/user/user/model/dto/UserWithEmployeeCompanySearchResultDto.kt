/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.user.user.model.dto

import com.bosch.pt.csm.cloud.common.Referable
import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.company.company.CompanyId
import com.bosch.pt.csm.company.employee.EmployeeId
import com.bosch.pt.csm.user.user.model.GenderEnum
import java.util.Date
import java.util.UUID

class UserWithEmployeeCompanySearchResultDto(
    val userIdentifier: UUID?,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val admin: Boolean,
    val locked: Boolean,
    val gender: GenderEnum?,
    val userCreatedDate: Date?,
    val companyIdentifier: CompanyId?,
    val companyName: String?,
    val employeeIdentifier: EmployeeId?
) : Referable {

  constructor(
      userIdentifier: UserId,
      firstName: String?,
      lastName: String?,
      email: String?,
      admin: Boolean,
      locked: Boolean,
      gender: GenderEnum?,
      userCreatedDate: Date?,
      companyIdentifier: CompanyId?,
      companyName: String?,
      employeeIdentifier: EmployeeId?
  ) : this(
      userIdentifier.toUuid(),
      firstName,
      lastName,
      email,
      admin,
      locked,
      gender,
      userCreatedDate,
      companyIdentifier,
      companyName,
      employeeIdentifier)

  override fun getIdentifierUuid(): UUID = employeeIdentifier!!.toUuid()

  override fun getDisplayName(): String = "$firstName $lastName"
}
