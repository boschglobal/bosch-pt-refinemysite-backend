/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.testdata

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.company.messages.StreetAddressAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser

fun EventStreamGenerator.plainProjectWithCsm(): EventStreamGenerator {
  submitCompany {
    it.streetAddress =
        StreetAddressAvro.newBuilder()
            .setStreet("Teststreet")
            .setHouseNumber("1")
            .setZipCode("12345")
            .setCity("Testtown")
            .setArea("BW")
            .setCountry("Germany")
            .build()
  }
  submitUser(asReference = "csm-user")
  submitEmployee(asReference = "csm-employee") { it.roles = listOf(CSM) }

  setUserContext("csm-user")
  submitProject()
  submitParticipantG3(asReference = "csm-participant")
  return this
}
