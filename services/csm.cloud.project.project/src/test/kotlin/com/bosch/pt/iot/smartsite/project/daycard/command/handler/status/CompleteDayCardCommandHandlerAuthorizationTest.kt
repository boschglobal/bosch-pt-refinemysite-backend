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
import com.bosch.pt.iot.smartsite.project.daycard.command.api.CompleteDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.batch.status.CompleteDayCardBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import java.time.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class CompleteDayCardCommandHandlerAuthorizationTest : AbstractDayCardAuthorizationTest() {

  @Autowired private lateinit var completeDayCardCommandHandler: CompleteDayCardCommandHandler
  @Autowired
  private lateinit var completeDayCardBatchCommandHandler: CompleteDayCardBatchCommandHandler

  private val dayCardOpen1Identifier by lazy { getIdentifier("dayCard1") }
  private val dayCardOpen2Identifier by lazy { getIdentifier("dayCard2") }
  private val dayCardIdentifiers by lazy {
    setOf(
        VersionedIdentifier(dayCardOpen1Identifier, 0L),
        VersionedIdentifier(dayCardOpen2Identifier, 0L))
  }

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
        .submitTaskSchedule(eventType = TaskScheduleEventEnumAvro.UPDATED) {
          it.start = LocalDate.now().toEpochMilli()
          it.end = LocalDate.now().plusDays(10).toEpochMilli()
          it.slots =
              listOf(
                  TaskScheduleSlotAvro(LocalDate.now().toEpochMilli(), getByReference("dayCard1")),
                  TaskScheduleSlotAvro(
                      LocalDate.now().plusDays(1).toEpochMilli(), getByReference("dayCard2")),
              )
        }
  }

  @ParameterizedTest
  @MethodSource("dayCardUpdatePermissionGroupWithAccess")
  fun `verify complete day card authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      completeDayCardCommandHandler.handle(
          CompleteDayCardCommand(dayCardOpen1Identifier.asDayCardId(), ETag.from("0")))
    }
  }

  @ParameterizedTest
  @MethodSource("dayCardUpdatePermissionGroupWithAccess")
  fun `verify complete multiple day cards authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) { completeDayCardBatchCommandHandler.handle(dayCardIdentifiers) }
  }
}
