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
import com.bosch.pt.iot.smartsite.project.daycard.command.api.ApproveDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.batch.status.ApproveDayCardBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import java.time.LocalDate.now
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class ApproveDayCardCommandHandlerAuthorizationTest : AbstractDayCardAuthorizationTest() {

  @Autowired
  private lateinit var approveDayCardBatchCommandHandler: ApproveDayCardBatchCommandHandler
  @Autowired private lateinit var approveDayCardCommandHandler: ApproveDayCardCommandHandler

  private val dayCardDone1Identifier by lazy { getIdentifier("dayCard1") }
  private val dayCardDone2Identifier by lazy { getIdentifier("dayCard2") }

  @BeforeEach
  fun init() {
    // Create a day card as csm and assign to fm
    authorizeWithUser(userCsm)
    eventStreamGenerator
        .setUserContext("userCsm")
        .submitProjectCraftG2()
        .submitTask { it.assignee = getByReference("participantFmAssignee") }
        .submitTaskSchedule()
        .submitDayCardG2("dayCard1") { it.status = DayCardStatusEnumAvro.DONE }
        .submitDayCardG2("dayCard2") { it.status = DayCardStatusEnumAvro.DONE }
        .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.start = now().toEpochMilli()
          it.end = now().plusDays(10).toEpochMilli()
          it.slots =
              listOf(
                  TaskScheduleSlotAvro(now().toEpochMilli(), getByReference("dayCard1")),
                  TaskScheduleSlotAvro(
                      now().plusDays(1).toEpochMilli(), getByReference("dayCard2")),
              )
        }
  }

  @ParameterizedTest
  @MethodSource("dayCardReviewPermissionGroupWithAccess")
  fun `verify approve day card authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      approveDayCardCommandHandler.handle(
          ApproveDayCardCommand(dayCardDone1Identifier.asDayCardId(), ETag.from("0")))
    }
  }

  @ParameterizedTest
  @MethodSource("dayCardReviewPermissionGroupWithAccess")
  fun `verify approve multiple day cards authorized for`(userType: UserTypeAccess) {
    val identifiers =
        setOf(
            VersionedIdentifier(dayCardDone1Identifier, 0L),
            VersionedIdentifier(dayCardDone2Identifier, 0L))
    checkAccessWith(userType) { approveDayCardBatchCommandHandler.handle(identifiers) }
  }
}
