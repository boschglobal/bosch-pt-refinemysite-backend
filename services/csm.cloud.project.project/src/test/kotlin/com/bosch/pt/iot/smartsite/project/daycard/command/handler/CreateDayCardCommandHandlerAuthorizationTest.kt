/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.handler

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleSlotAvro
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.authorizeWithUser
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.daycard.command.api.CreateDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.status.AbstractDayCardAuthorizationTest
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class CreateDayCardCommandHandlerAuthorizationTest : AbstractDayCardAuthorizationTest() {

  @Autowired private lateinit var createDayCardCommandHandler: CreateDayCardCommandHandler

  private val taskIdentifier by lazy { getIdentifier("task").asTaskId() }

  @BeforeEach
  fun init() {
    // Create a day cards as csm and assign to fm
    authorizeWithUser(userCsm)
    eventStreamGenerator
        .setUserContext("userCsm")
        .submitProjectCraftG2()
        .submitTask { it.assignee = getByReference("participantFmAssignee") }
        .submitTaskSchedule()
        .submitDayCardG2("dayCard1") { it.status = DayCardStatusEnumAvro.OPEN }
        .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.start = LocalDate.now().toEpochMilli()
          it.end = LocalDate.now().plusDays(10).toEpochMilli()
          it.slots =
              listOf(
                  TaskScheduleSlotAvro(LocalDate.now().toEpochMilli(), getByReference("dayCard1")),
              )
        }
  }

  @ParameterizedTest
  @MethodSource("dayCardUpdatePermissionGroupWithAccess")
  fun `verify create day card authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      createDayCardCommandHandler.handle(
          CreateDayCardCommand(
              identifier = DayCardId(),
              taskIdentifier = taskIdentifier,
              title = "Title of the day card",
              manpower = BigDecimal("10.3"),
              notes = "Notes of the day card",
              status = DayCardStatusEnum.OPEN))
    }
  }
}
