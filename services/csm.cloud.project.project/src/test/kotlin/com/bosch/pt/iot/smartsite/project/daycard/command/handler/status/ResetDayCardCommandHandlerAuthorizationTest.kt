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
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleSlotAvro
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.authorizeWithUser
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.daycard.command.api.ResetDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.batch.status.ResetDayCardBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import java.time.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class ResetDayCardCommandHandlerAuthorizationTest : AbstractDayCardAuthorizationTest() {

  @Autowired private lateinit var resetDayCardCommandHandler: ResetDayCardCommandHandler
  @Autowired private lateinit var resetDayCardBatchCommandHandler: ResetDayCardBatchCommandHandler

  private val dayCard1NotDoneIdentifier by lazy { getIdentifier("dayCard3") }
  private val dayCard2NotDoneIdentifier by lazy { getIdentifier("dayCard4") }
  private val dayCard1DoneIdentifier by lazy { getIdentifier("dayCard5") }
  private val dayCard2DoneIdentifier by lazy { getIdentifier("dayCard6") }
  private val dayCard1ApprovedIdentifier by lazy { getIdentifier("dayCard7") }
  private val dayCard2ApprovedIdentifier by lazy { getIdentifier("dayCard8") }

  @BeforeEach
  fun init() {
    // Create a day cards as csm and assign to fm
    authorizeWithUser(userCsm)
    eventStreamGenerator
        .setUserContext("userCsm")
        .submitProjectCraftG2()
        .submitTask {
          it.assignee = EventStreamGeneratorStaticExtensions.getByReference("participantFmAssignee")
        }
        .submitTaskSchedule()
        .submitDayCardG2("dayCard1") { it.status = DayCardStatusEnumAvro.OPEN }
        .submitDayCardG2("dayCard2") { it.status = DayCardStatusEnumAvro.OPEN }
        .submitDayCardG2("dayCard3") {
          it.status = DayCardStatusEnumAvro.NOTDONE
          it.reason = DayCardReasonNotDoneEnumAvro.BAD_WEATHER
        }
        .submitDayCardG2("dayCard4") {
          it.status = DayCardStatusEnumAvro.NOTDONE
          it.reason = DayCardReasonNotDoneEnumAvro.BAD_WEATHER
        }
        .submitDayCardG2("dayCard5") { it.status = DayCardStatusEnumAvro.DONE }
        .submitDayCardG2("dayCard6") { it.status = DayCardStatusEnumAvro.DONE }
        .submitDayCardG2("dayCard7") { it.status = DayCardStatusEnumAvro.APPROVED }
        .submitDayCardG2("dayCard8") { it.status = DayCardStatusEnumAvro.APPROVED }
        .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.start = LocalDate.now().toEpochMilli()
          it.end = LocalDate.now().plusDays(10).toEpochMilli()
          it.slots =
              listOf(
                  TaskScheduleSlotAvro(
                      LocalDate.now().toEpochMilli(),
                      EventStreamGeneratorStaticExtensions.getByReference("dayCard1")),
                  TaskScheduleSlotAvro(
                      LocalDate.now().plusDays(1).toEpochMilli(),
                      EventStreamGeneratorStaticExtensions.getByReference("dayCard2")),
                  TaskScheduleSlotAvro(
                      LocalDate.now().plusDays(2).toEpochMilli(),
                      EventStreamGeneratorStaticExtensions.getByReference("dayCard3")),
                  TaskScheduleSlotAvro(
                      LocalDate.now().plusDays(3).toEpochMilli(),
                      EventStreamGeneratorStaticExtensions.getByReference("dayCard4")),
                  TaskScheduleSlotAvro(
                      LocalDate.now().plusDays(4).toEpochMilli(),
                      EventStreamGeneratorStaticExtensions.getByReference("dayCard5")),
                  TaskScheduleSlotAvro(
                      LocalDate.now().plusDays(5).toEpochMilli(),
                      EventStreamGeneratorStaticExtensions.getByReference("dayCard6")),
                  TaskScheduleSlotAvro(
                      LocalDate.now().plusDays(6).toEpochMilli(),
                      EventStreamGeneratorStaticExtensions.getByReference("dayCard7")),
                  TaskScheduleSlotAvro(
                      LocalDate.now().plusDays(7).toEpochMilli(),
                      EventStreamGeneratorStaticExtensions.getByReference("dayCard8")),
              )
        }
  }

  // Not Done -> Reset -> Open
  @ParameterizedTest
  @MethodSource("dayCardReviewPermissionGroupWithAccess")
  fun `verify reset status from not done authorized for csm`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      resetDayCardCommandHandler.handle(
          ResetDayCardCommand(dayCard1NotDoneIdentifier.asDayCardId(), ETag.from("0")))
    }
  }

  // Done -> Reset -> Open
  @ParameterizedTest
  @MethodSource("dayCardReviewPermissionGroupWithAccess")
  fun `verify reset status from done authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      resetDayCardCommandHandler.handle(
          ResetDayCardCommand(dayCard1DoneIdentifier.asDayCardId(), ETag.from("0")))
    }
  }

  // Approved -> Reset -> Open
  @ParameterizedTest
  @MethodSource("dayCardReviewPermissionGroupWithAccess")
  fun `verify reset status from approved authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      resetDayCardCommandHandler.handle(
          ResetDayCardCommand(dayCard1ApprovedIdentifier.asDayCardId(), ETag.from("0")))
    }
  }

  // Not Done -> Reset -> Open
  @ParameterizedTest
  @MethodSource("dayCardReviewPermissionGroupWithAccess")
  fun `verify reset multiple day cards from not done authorized for csm`(userType: UserTypeAccess) {
    val notDoneDayCardIdentifiers =
        setOf(
            VersionedIdentifier(dayCard1NotDoneIdentifier, 0L),
            VersionedIdentifier(dayCard2NotDoneIdentifier, 0L))
    checkAccessWith(userType) { resetDayCardBatchCommandHandler.handle(notDoneDayCardIdentifiers) }
  }

  // Done -> Reset -> Open
  @ParameterizedTest
  @MethodSource("dayCardReviewPermissionGroupWithAccess")
  fun `verify reset multiple day cards from done authorized for`(userType: UserTypeAccess) {
    val doneDayCardIdentifiers =
        setOf(
            VersionedIdentifier(dayCard1DoneIdentifier, 0L),
            VersionedIdentifier(dayCard2DoneIdentifier, 0L))
    checkAccessWith(userType) { resetDayCardBatchCommandHandler.handle(doneDayCardIdentifiers) }
  }

  // Approved -> Reset -> Open
  @ParameterizedTest
  @MethodSource("dayCardReviewPermissionGroupWithAccess")
  fun `verify reset multiple day cards approved authorized for`(userType: UserTypeAccess) {
    val approvedDayCardIdentifiers =
        setOf(
            VersionedIdentifier(dayCard1ApprovedIdentifier, 0L),
            VersionedIdentifier(dayCard2ApprovedIdentifier, 0L))
    checkAccessWith(userType) { resetDayCardBatchCommandHandler.handle(approvedDayCardIdentifiers) }
  }
}
