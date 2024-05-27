/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.query

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
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.status.AbstractDayCardAuthorizationTest
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class DayCardQueryServiceAuthorizationTest : AbstractDayCardAuthorizationTest() {

  @Autowired private lateinit var cut: DayCardQueryService

  private val taskIdentifier by lazy { getIdentifier("task").asTaskId() }
  private val dayCardOpenIdentifier by lazy { getIdentifier("dayCard1").asDayCardId() }
  private val dayCardDoneIdentifier by lazy { getIdentifier("dayCard2").asDayCardId() }

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
        .submitDayCardG2("dayCard2") { it.status = DayCardStatusEnumAvro.DONE }
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
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify find by identifier authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      val dayCard = cut.findByIdentifier(dayCardOpenIdentifier)
      val dayCardWithDetails = cut.findWithDetails(dayCardOpenIdentifier)
      val dayCardsWithDetails = cut.findAllWithDetailsByTaskIdentifier(taskIdentifier)
      println(taskIdentifier)
      assertThat(dayCard).isNotNull
      assertThat(dayCardWithDetails).isNotNull
      assertThat(dayCardsWithDetails).isNotEmpty()
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `verify find by identifier not authorized non existing day card`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.findByIdentifier(DayCardId()) }
    checkAccessWith(userType) { cut.findWithDetails(DayCardId()) }
    checkAccessWith(userType) { cut.findAllWithDetailsByTaskIdentifier(TaskId()) }
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify find all by identifiers authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      val dayCardWithDetails: Collection<DayCard> =
          cut.findAllWithDetailsByTaskIdentifiers(setOf(taskIdentifier))
      assertThat(dayCardWithDetails).isNotEmpty()
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `verify find all by identifier not authorized non existing task`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.findAllWithDetailsByTaskIdentifiers(setOf(TaskId())) }
  }

  @ParameterizedTest
  @MethodSource("allActiveParticipantsWithAccess")
  fun `verify find all projected by identifier authorized for`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.findAllByIdentifierIn(setOf(dayCardOpenIdentifier, dayCardDoneIdentifier))
    }
  }

  @ParameterizedTest
  @MethodSource("noOneWithoutAdminAccess")
  fun `verify find all projected by identifier not authorized non existing task`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) { cut.findAllByIdentifierIn(setOf(DayCardId())) }
  }
}
