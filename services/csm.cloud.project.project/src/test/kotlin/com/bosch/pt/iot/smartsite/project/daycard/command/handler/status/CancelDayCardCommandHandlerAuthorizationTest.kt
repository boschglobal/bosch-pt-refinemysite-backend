/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.handler.status

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.VersionedIdentifier
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
import com.bosch.pt.iot.smartsite.project.daycard.command.api.CancelDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.batch.status.CancelDayCardBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum.BAD_WEATHER
import java.time.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class CancelDayCardCommandHandlerAuthorizationTest : AbstractDayCardAuthorizationTest() {

  @Autowired private lateinit var cancelDayCardCommandHandler: CancelDayCardCommandHandler
  @Autowired private lateinit var cancelDayCardBatchCommandHandler: CancelDayCardBatchCommandHandler

  private val dayCardOpen1Identifier by lazy { getIdentifier("dayCard1") }
  private val dayCardOpen2Identifier by lazy { getIdentifier("dayCard2") }
  private val dayCardDone1Identifier by lazy { getIdentifier("dayCard3") }
  private val dayCardDone2Identifier by lazy { getIdentifier("dayCard4") }

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
        .submitDayCardG2("dayCard2") { it.status = DayCardStatusEnumAvro.OPEN }
        .submitDayCardG2("dayCard3") { it.status = DayCardStatusEnumAvro.DONE }
        .submitDayCardG2("dayCard4") { it.status = DayCardStatusEnumAvro.DONE }
        .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.start = LocalDate.now().toEpochMilli()
          it.end = LocalDate.now().plusDays(10).toEpochMilli()
          it.slots =
              listOf(
                  TaskScheduleSlotAvro(LocalDate.now().toEpochMilli(), getByReference("dayCard1")),
                  TaskScheduleSlotAvro(
                      LocalDate.now().plusDays(1).toEpochMilli(), getByReference("dayCard2")),
                  TaskScheduleSlotAvro(
                      LocalDate.now().plusDays(2).toEpochMilli(), getByReference("dayCard3")),
                  TaskScheduleSlotAvro(
                      LocalDate.now().plusDays(3).toEpochMilli(), getByReference("dayCard4")),
              )
        }
  }

  // Open -> Not Done
  @ParameterizedTest
  @MethodSource("dayCardUpdatePermissionGroupWithAccess")
  fun `verify cancel day card from open authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cancelDayCardCommandHandler.handle(
          CancelDayCardCommand(dayCardOpen1Identifier.asDayCardId(), BAD_WEATHER, ETag.from("0")))
    }
  }

  // Done -> Not Done
  @ParameterizedTest
  @MethodSource("dayCardReviewPermissionGroupWithAccess")
  fun `verify cancel day card from done authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cancelDayCardCommandHandler.handle(
          CancelDayCardCommand(dayCardDone1Identifier.asDayCardId(), BAD_WEATHER, ETag.from("0")))
    }
  }

  // Open -> Not Done
  @ParameterizedTest
  @MethodSource("dayCardUpdatePermissionGroupWithAccess")
  fun `verify cancel multiple day cards from open authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cancelDayCardBatchCommandHandler.handle(
          setOf(
              VersionedIdentifier(dayCardOpen1Identifier, 0L),
              VersionedIdentifier(dayCardOpen2Identifier, 0L)),
          BAD_WEATHER)
    }
  }

  // Done -> Not Done
  @ParameterizedTest
  @MethodSource("dayCardReviewPermissionGroupWithAccess")
  fun `verify cancel multiple day cards from done authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cancelDayCardBatchCommandHandler.handle(
          setOf(
              VersionedIdentifier(dayCardDone1Identifier, 0L),
              VersionedIdentifier(dayCardDone2Identifier, 0L)),
          BAD_WEATHER)
    }
  }

  // 1 Open / 1 Done -> Not Done
  @ParameterizedTest
  @MethodSource("dayCardReviewPermissionGroupWithAccess")
  fun `verify cancel multiple day cards from open and done authorized for`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) {
      cancelDayCardBatchCommandHandler.handle(
          setOf(
              VersionedIdentifier(dayCardOpen1Identifier, 0L),
              VersionedIdentifier(dayCardDone2Identifier, 0L)),
          BAD_WEATHER)
    }
  }
}
