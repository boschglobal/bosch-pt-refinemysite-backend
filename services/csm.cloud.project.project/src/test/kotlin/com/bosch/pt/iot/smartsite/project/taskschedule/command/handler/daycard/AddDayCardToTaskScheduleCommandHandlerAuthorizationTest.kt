/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.daycard

import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.iot.smartsite.common.facade.rest.UserTypeAccess
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.toAggregateReference
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.AddDayCardsToTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.AbstractTaskScheduleAuthorizationTest
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import java.time.LocalDate
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class AddDayCardToTaskScheduleCommandHandlerAuthorizationTest :
    AbstractTaskScheduleAuthorizationTest() {

  @Autowired
  private lateinit var addDayCardToTaskScheduleCommandHandler:
      AddDayCardToTaskScheduleCommandHandler

  @ParameterizedTest
  @MethodSource("csmAndCrAndCreatorWithAccess")
  fun `add day card to task schedule permission is granted to`(userType: UserTypeAccess) {
    eventStreamGenerator.submitDayCardG2("someDayCard") {
      it.task = taskSchedule.task.identifier.toAggregateReference()
    }

    val dayCardIdentifier = EventStreamGeneratorStaticExtensions.getIdentifier("someDayCard")

    checkAccessWith(userType) {
      addDayCardToTaskScheduleCommandHandler.handle(
          AddDayCardsToTaskScheduleCommand(
              taskScheduleIdentifier = taskSchedule.identifier,
              projectIdentifier = taskSchedule.project.identifier,
              taskIdentifier = taskWithScheduleIdentifier,
              date = LocalDate.now(),
              dayCardIdentifier = dayCardIdentifier.asDayCardId(),
              eTag = ETag.from("0")))
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `add day card to task schedule permission is denied for non-existing task`(
      userType: UserTypeAccess
  ) {
    eventStreamGenerator.submitDayCardG2("someDayCard") {
      it.task = taskSchedule.task.identifier.toAggregateReference()
    }

    val dayCardIdentifier = EventStreamGeneratorStaticExtensions.getIdentifier("someDayCard")

    checkAccessWith(userType) {
      addDayCardToTaskScheduleCommandHandler.handle(
          AddDayCardsToTaskScheduleCommand(
              TaskScheduleId(),
              ProjectId(),
              TaskId(),
              LocalDate.now(),
              dayCardIdentifier.asDayCardId(),
              ETag.from("0")))
    }
  }
}
