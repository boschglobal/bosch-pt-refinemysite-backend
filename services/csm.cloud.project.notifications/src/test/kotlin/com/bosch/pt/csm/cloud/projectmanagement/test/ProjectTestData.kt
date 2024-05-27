/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.test

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate

fun EventStreamGenerator.submitInitProjectData() =
    submitSystemUserAndActivate()
        .submitCompany()
        .submitUserAndActivate(asReference = "csm-user")
        .submitEmployee { it.roles = listOf(EmployeeRoleEnumAvro.CSM) }
        .submitProject()
        .submitProjectCraftG2()
        .submitParticipantG3(asReference = "csm-participant") {
          it.role = ParticipantRoleEnumAvro.CSM
        }
        .submitUser("fm-user")
        .submitEmployee { it.roles = listOf(EmployeeRoleEnumAvro.FM) }
        .submitParticipantG3(asReference = "fm-participant") {
          it.role = ParticipantRoleEnumAvro.FM
        }
        .run { this }
