/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser

fun EventStreamGenerator.setupRfvIntegrationTestData(): EventStreamGenerator {
  submitCompany()
  submitUser(asReference = "csm-user")
  submitEmployee(asReference = "csm-employee") { it.roles = listOf(EmployeeRoleEnumAvro.CSM) }

  setUserContext("csm-user")
  submitProject()
  submitParticipantG3(asReference = "csm-participant")
  return this
}
