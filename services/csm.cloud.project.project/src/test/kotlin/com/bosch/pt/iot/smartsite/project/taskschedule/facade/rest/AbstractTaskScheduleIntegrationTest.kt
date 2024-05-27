/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.APPROVED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro.OPEN
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleSlotAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.asTaskScheduleId
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.request.TaskScheduleSlotDto
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskScheduleSlot
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository.TaskScheduleRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDate.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
open class AbstractTaskScheduleIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired lateinit var cut: TaskScheduleController

  @Autowired lateinit var taskScheduleRepository: TaskScheduleRepository

  protected val taskWithoutSchedule by lazy {
    repositories.findTaskWithDetails(getIdentifier("taskWithoutSchedule").asTaskId())!!
  }
  protected val taskWithoutSchedule2 by lazy {
    repositories.findTaskWithDetails(getIdentifier("taskWithoutSchedule2").asTaskId())!!
  }
  protected val taskWithoutScheduleFromOtherProject by lazy {
    repositories.findTaskWithDetails(
        getIdentifier("taskWithoutScheduleFromOtherProject").asTaskId())!!
  }
  protected val taskWithScheduleFromOtherProject by lazy {
    repositories.findTaskWithDetails(getIdentifier("taskWithScheduleFromOtherProject").asTaskId())!!
  }
  protected val taskScheduleWithTwoDayCards by lazy {
    repositories.findTaskScheduleWithDetails(
        getIdentifier("taskScheduleWithTwoDayCards").asTaskScheduleId())!!
  }
  protected val taskScheduleWithoutDayCard by lazy {
    repositories.findTaskScheduleWithDetails(
        getIdentifier("taskScheduleWithoutDayCard").asTaskScheduleId())!!
  }
  protected val taskScheduleWithNonOpenDayCard by lazy {
    repositories.findTaskScheduleWithDetails(
        getIdentifier("taskScheduleWithNonOpenDayCard").asTaskScheduleId())!!
  }

  protected val defaultShift = 7
  protected val defaultStartDate: LocalDate = now()
  protected val defaultEndDate: LocalDate = defaultStartDate.plusDays(10)
  protected val defaultShiftedStartDate: LocalDate =
      defaultStartDate.plusDays(defaultShift.toLong())
  protected val defaultShiftedEndDate: LocalDate = defaultEndDate.plusDays(defaultShift.toLong())
  protected val dayCard2Identifier: DayCardId by lazy { getIdentifier("dayCard2").asDayCardId() }
  protected val dayCardApprovedIdentifier: DayCardId by lazy {
    getIdentifier("dayCardApproved").asDayCardId()
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitTask(randomString())
        .submitTaskSchedule("taskScheduleWithTwoDayCards")
        .submitDayCardG2("dayCard1") {
          it.manpower = BigDecimal("3.50")
          it.status = OPEN
        }
        .submitDayCardG2("dayCard2") { it.status = OPEN }
        .submitTaskSchedule("taskScheduleWithTwoDayCards", eventType = UPDATED) {
          it.start = defaultStartDate.toEpochMilli()
          it.end = defaultEndDate.toEpochMilli()
          it.slots =
              listOf(
                  TaskScheduleSlotAvro(defaultStartDate.toEpochMilli(), getByReference("dayCard1")),
                  TaskScheduleSlotAvro(
                      defaultStartDate.plusDays(1).toEpochMilli(), getByReference("dayCard2")),
              )
        }
        .submitTask(randomString())
        .submitTaskSchedule("taskScheduleWithNonOpenDayCard")
        .submitDayCardG2("dayCardApproved") {
          it.status = APPROVED
          it.manpower = BigDecimal("3.50")
        }
        .submitTaskSchedule("taskScheduleWithNonOpenDayCard", eventType = UPDATED) {
          it.start = defaultStartDate.toEpochMilli()
          it.end = defaultEndDate.toEpochMilli()
          it.slots =
              listOf(
                  TaskScheduleSlotAvro(
                      defaultStartDate.toEpochMilli(), getByReference("dayCardApproved")))
        }
        .submitTask(randomString())
        .submitTaskSchedule("taskScheduleWithoutDayCard") {
          it.start = defaultStartDate.toEpochMilli()
          it.end = defaultEndDate.toEpochMilli()
        }
        .submitTask("taskWithoutSchedule")
        .submitTask("taskWithoutSchedule2")
        .submitProject("otherProject")
        .submitParticipantG3("otherProjectParticipant") {
          it.project = getByReference("otherProject")
          it.user = getByReference("userCsm1")
          it.role = CSM
        }
        .submitTask("taskWithoutScheduleFromOtherProject")
        .submitTask("taskWithScheduleFromOtherProject")
        .submitTaskSchedule(randomString()) {
          it.start = defaultStartDate.toEpochMilli()
          it.end = defaultEndDate.toEpochMilli()
        }

    setAuthentication("userCsm1")
    projectEventStoreUtils.reset()
  }

  protected open fun shiftSlots(
      taskSchedule: TaskSchedule,
      daysToShift: Int
  ): List<TaskScheduleSlotDto> =
      taskSchedule.slots!!.map { slot: TaskScheduleSlot ->
        TaskScheduleSlotDto(slot.dayCard.identifier, slot.date.plusDays(daysToShift.toLong()))
      }

  protected open fun removeSlot(
      taskSchedule: TaskSchedule?,
      identifier: DayCardId
  ): List<TaskScheduleSlotDto> =
      taskSchedule!!
          .slots!!
          .map { slot: TaskScheduleSlot -> TaskScheduleSlotDto(slot.dayCard.identifier, slot.date) }
          .filter { slot: TaskScheduleSlotDto -> slot.id != identifier }

  protected open fun changeDateOfSlot(
      taskSchedule: TaskSchedule,
      identifier: DayCardId,
      date: LocalDate?
  ): List<TaskScheduleSlotDto> =
      taskSchedule.slots!!.map { slot: TaskScheduleSlot ->
        TaskScheduleSlotDto(
            slot.dayCard.identifier,
            (if (slot.dayCard.identifier == identifier) date else slot.date)!!)
      }

  protected fun changeIdentifierOfSlot(
      taskSchedule: TaskSchedule,
      oldIdentifier: DayCardId,
      newIdentifier: DayCardId?
  ): List<TaskScheduleSlotDto> =
      taskSchedule.slots!!.map { slot: TaskScheduleSlot ->
        TaskScheduleSlotDto(
            (if (slot.dayCard.identifier == oldIdentifier) newIdentifier
            else slot.dayCard.identifier)!!,
            slot.date)
      }

  protected fun assertSlotsAreOrdered(taskSchedule: TaskSchedule) {
    var previousSlotDate: LocalDate? = null
    taskSchedule.slots!!.forEach {
      if (previousSlotDate != null) {
        assertThat(previousSlotDate).isBefore(it.date)
      }
      previousSlotDate = it.date
    }
  }
}
