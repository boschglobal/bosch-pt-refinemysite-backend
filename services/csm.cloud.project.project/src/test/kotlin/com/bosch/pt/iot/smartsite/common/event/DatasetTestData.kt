/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.event

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestoneList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRelation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkdayConfiguration
import com.bosch.pt.csm.cloud.usermanagement.craft.event.submitCraft
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser

fun EventStreamGenerator.setupDatasetTestData(): EventStreamGenerator {
  submitSystemUserAndActivate()
  submitUser(asReference = "admin") {
    it.admin = true
    it.firstName = "Test"
    it.lastName = "Admin"
  }
  submitCraft()
  submitUser(asReference = "user") {
    it.firstName = "Ali"
    it.lastName = "Albatros"
  }
  submitUser(asReference = "userCsm1") {
    it.firstName = "Daniel"
    it.lastName = "Düsentrieb"
  }
  submitUser(asReference = "userCsm2") {
    it.firstName = "Eva"
    it.lastName = "Engelbrecht"
  }

  setUserContext(name = "system")
  submitCompany()
  submitEmployee(asReference = "employee") {
    it.user = getByReference("user")
    it.roles = listOf(EmployeeRoleEnumAvro.FM)
  }
  submitEmployee(asReference = "employeeCsm1") {
    it.user = getByReference("userCsm1")
    it.roles = listOf(EmployeeRoleEnumAvro.CSM)
  }
  submitEmployee(asReference = "employeeCsm2") {
    it.user = getByReference("userCsm2")
    it.roles = listOf(EmployeeRoleEnumAvro.CSM)
  }

  setUserContext(name = "userCsm1")
  submitProject()
  submitWorkdayConfiguration()
  submitWorkArea()
  submitWorkAreaList()
  submitParticipantG3(asReference = "participantCsm1") {
    it.user = getByReference("userCsm1")
    it.role = ParticipantRoleEnumAvro.CSM
  }
  submitParticipantG3(asReference = "participantCsm2") {
    it.user = getByReference("userCsm2")
    it.role = ParticipantRoleEnumAvro.CSM
  }
  submitParticipantG3(asReference = "participant") {
    it.user = getByReference("user")
    it.role = ParticipantRoleEnumAvro.FM
  }
  submitProjectCraftG2()
  submitProjectCraftList()
  submitMilestone()
  submitMilestoneList()
  submitTask(asReference = "task") { it.assignee = getByReference("participant") }
  submitTaskSchedule(asReference = "taskSchedule")
  submitTopicG2(asReference = "topic")
  submitMessage(asReference = "message")
  submitRelation(asReference = "relation")

  return this
}
