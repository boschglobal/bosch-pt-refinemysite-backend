/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.handler

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
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
import com.bosch.pt.iot.smartsite.project.daycard.command.api.UpdateDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.status.AbstractDayCardAuthorizationTest
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class UpdateDayCardCommandHandlerAuthorizationTest : AbstractDayCardAuthorizationTest() {

  @Autowired private lateinit var updateDayCardCommandHandler: UpdateDayCardCommandHandler

  private val dayCardOpenIdentifier by lazy { getIdentifier("dayCard1").asDayCardId() }

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
  fun `verify update day card authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      updateDayCardCommandHandler.handle(
          UpdateDayCardCommand(
              identifier = dayCardOpenIdentifier,
              title = "Updated title of the day card",
              manpower = BigDecimal("10.3"),
              notes = "Notes of the day card",
              eTag = ETag.from("0")))
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `verify update day card not authorized non existing day card`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      updateDayCardCommandHandler.handle(
          UpdateDayCardCommand(
              identifier = DayCardId(),
              title = "New title of the day card",
              manpower = BigDecimal("10.3"),
              notes = "Notes of the day card",
              eTag = ETag.from("0")))
    }
  }
}
