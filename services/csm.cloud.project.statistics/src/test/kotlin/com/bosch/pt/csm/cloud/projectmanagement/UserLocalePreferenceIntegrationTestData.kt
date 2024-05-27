/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.util.asSlot
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import java.time.LocalDate

fun EventStreamGenerator.submitUserLocalePreferenceIntegrationTestData(): EventStreamGenerator {
  submitCompany()
  submitUser(asReference = "csmUser")
  submitEmployee(asReference = "csmEmployee") { it.roles = listOf(EmployeeRoleEnumAvro.CSM) }

  setUserContext("csmUser")
  submitProject()
  submitProjectCraftG2()
  submitParticipantG3(asReference = "csmParticipant")
  submitTask()
  submitTaskSchedule {
    it.start = LocalDate.now().toEpochMilli()
    it.end = LocalDate.now().minusDays(10).toEpochMilli()
  }
  submitDayCardG2 { it.status = DayCardStatusEnumAvro.OPEN }
  submitDayCardG2(eventType = DayCardEventEnumAvro.UPDATED) {
    it.status = DayCardStatusEnumAvro.NOTDONE
    it.reason = DayCardReasonNotDoneEnumAvro.DELAYED_MATERIAL
  }
  submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
    it.slots = listOf(getByReference("dayCard").asSlot(LocalDate.now().minusDays(9)))
  }
  return this
}
