/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common

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

const val CSM_PARTICIPANT = "csm-participant"
const val CSM_USER = "csm-user"
const val FM_PARTICIPANT = "fm-participant"
const val FM_USER = "fm-user"

fun EventStreamGenerator.submitInitProjectData() =
    submitSystemUserAndActivate()
        .submitCompany()
        .submitUserAndActivate(asReference = CSM_USER)
        .submitEmployee { it.roles = listOf(EmployeeRoleEnumAvro.CSM) }
        .submitProject()
        .submitProjectCraftG2()
        .submitParticipantG3(asReference = CSM_PARTICIPANT) {
          it.role = ParticipantRoleEnumAvro.CSM
        }
        .submitUser(FM_USER)
        .submitEmployee { it.roles = listOf(EmployeeRoleEnumAvro.FM) }
        .submitParticipantG3(asReference = FM_PARTICIPANT) { it.role = ParticipantRoleEnumAvro.FM }
        .run { this }
