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
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.toEtag
import com.bosch.pt.iot.smartsite.project.daycard.command.api.DeleteDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.api.DeleteDayCardsFromScheduleCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.DeleteDayCardCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.batch.DeleteDayCardBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.command.service.TaskScheduleService
import java.time.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class DeleteDayCardCommandHandlerAuthorizationTest : AbstractDayCardAuthorizationTest() {

  @Autowired private lateinit var deleteDayCardCommandHandler: DeleteDayCardCommandHandler
  @Autowired private lateinit var deleteDayCardBatchCommandHandler: DeleteDayCardBatchCommandHandler

  @Autowired private lateinit var taskScheduleService: TaskScheduleService

  private val taskIdentifier by lazy { getIdentifier("task").asTaskId() }
  private val dayCardOpenIdentifier by lazy { getIdentifier("dayCard1") }

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
  fun `verify delete day card authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      val schedule = taskScheduleService.findByTaskIdentifier(taskIdentifier)
      deleteDayCardCommandHandler.handle(
          DeleteDayCardCommand(dayCardOpenIdentifier.asDayCardId(), schedule.toEtag()))
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `verify delete day card not authorized non existing day card`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      deleteDayCardCommandHandler.handle(DeleteDayCardCommand(DayCardId(), ETag.from("0")))
    }
  }

  @ParameterizedTest
  @MethodSource("dayCardUpdatePermissionGroupWithAccess")
  fun `verify delete day cards authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      val schedule = taskScheduleService.findByTaskIdentifier(taskIdentifier)
      deleteDayCardBatchCommandHandler.handle(
          listOf(
              DeleteDayCardsFromScheduleCommand(
                  setOf(dayCardOpenIdentifier.asDayCardId()), schedule.toEtag())),
          project.identifier)
    }
  }
}
