/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.extensions

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleSlotAvro
import java.time.LocalDate

fun EventStreamGenerator.submitTaskScheduleWithDayCardsG2(
    taskName: String,
    dayCards: Map<LocalDate, Pair<DayCardStatusEnumAvro, DayCardReasonNotDoneEnumAvro?>>,
    scheduleStart: LocalDate? = dayCards.keys.minOrNull(),
    scheduleEnd: LocalDate? = dayCards.keys.maxOrNull(),
) {
  var counter = 0
  val slots =
      dayCards
          .mapValues { status ->
            submitDayCardG2UntilState(
                    name = "$taskName-daycard-$counter",
                    taskName = taskName,
                    dayCardStatus = status.value.first,
                    notDoneReason = status.value.second)
                .also { counter += 1 }
          }
          .mapValues { entry ->
            TaskScheduleSlotAvro.newBuilder()
                .setDate(entry.key.toEpochMilli())
                .setDayCard(getByReference(entry.value))
                .build()
          }
          .values
          .toList()

  submitTaskSchedule(
      asReference = "$taskName-schedule",
      aggregateModifications = {
        it.task = getByReference(taskName)
        it.start = scheduleStart?.toEpochMilli()
        it.end = scheduleEnd?.toEpochMilli()
        it.slots = slots
      })
}

fun EventStreamGenerator.submitDayCardsInDifferentWeeks(
    startDateWeekOne: LocalDate,
    differenceInWeeks: Long
) {
  val startDateWeekTwo = startDateWeekOne.plusWeeks(differenceInWeeks)
  val dayCards =
      mapOf(
          startDateWeekOne to Pair(DayCardStatusEnumAvro.DONE, null),
          startDateWeekOne.plusDays(1) to Pair(DayCardStatusEnumAvro.APPROVED, null),
          startDateWeekOne.plusDays(2) to Pair(DayCardStatusEnumAvro.NOTDONE, null),
          startDateWeekOne.plusDays(3) to Pair(DayCardStatusEnumAvro.OPEN, null),
          startDateWeekTwo to Pair(DayCardStatusEnumAvro.DONE, null),
          startDateWeekTwo.plusDays(1) to Pair(DayCardStatusEnumAvro.APPROVED, null),
          startDateWeekTwo.plusDays(2) to Pair(DayCardStatusEnumAvro.NOTDONE, null),
          startDateWeekTwo.plusDays(3) to Pair(DayCardStatusEnumAvro.OPEN, null))
  submitTaskScheduleWithDayCardsG2("task-1", dayCards)
}

fun EventStreamGenerator.submitDayCardsInSameWeek(startDate: LocalDate) {
  val dayCards =
      mapOf(
          startDate to Pair(DayCardStatusEnumAvro.DONE, null),
          startDate.plusDays(1) to Pair(DayCardStatusEnumAvro.APPROVED, null),
          startDate.plusDays(2) to Pair(DayCardStatusEnumAvro.NOTDONE, null),
          startDate.plusDays(3) to Pair(DayCardStatusEnumAvro.OPEN, null))
  submitTaskScheduleWithDayCardsG2("task-1", dayCards)
}

fun EventStreamGenerator.submitDayCardsWithState(
    startDate: LocalDate,
    state: DayCardStatusEnumAvro
) {
  val dayCards =
      mapOf(
          startDate to Pair(state, null),
          startDate.plusDays(1) to Pair(state, null),
          startDate.plusDays(2) to Pair(state, null))
  submitTaskScheduleWithDayCardsG2("task-1", dayCards)
}

fun EventStreamGenerator.submitDayCardsForDifferentTasks(startDate: LocalDate) {
  submitTask(asReference = "another-task") {
    it.craft = getByReference("craft-1")
    it.assignee = getByReference("participant-fm-a")
  }

  val dayCardsOfTask =
      mapOf(
          startDate to Pair(DayCardStatusEnumAvro.APPROVED, null),
          startDate.plusDays(1) to Pair(DayCardStatusEnumAvro.NOTDONE, null),
          startDate.plusDays(2) to Pair(DayCardStatusEnumAvro.OPEN, null))
  submitTaskScheduleWithDayCardsG2("task-1", dayCardsOfTask)
  submitTaskScheduleWithDayCardsG2(
      "another-task", mapOf(Pair(startDate, Pair(DayCardStatusEnumAvro.DONE, null))))
}

fun EventStreamGenerator.submitScheduleWithDayCards(
    taskName: String,
    startDate: LocalDate
): AggregateIdentifierAvro {
  val dayCards =
      mapOf(
          startDate.plusDays(1) to Pair(DayCardStatusEnumAvro.OPEN, null),
          startDate.plusDays(3) to Pair(DayCardStatusEnumAvro.DONE, null))
  submitTaskScheduleWithDayCardsG2(taskName, dayCards, startDate, startDate.plusWeeks(1))
  return getByReference("$taskName-daycard-0")
}

private fun EventStreamGenerator.submitDayCardForOpenStatus(
    name: String,
    taskName: String,
) {
  submitDayCardG2(asReference = name, eventType = DayCardEventEnumAvro.CREATED) {
    it.status = DayCardStatusEnumAvro.OPEN
    it.task = getByReference(taskName)
  }
}

private fun EventStreamGenerator.submitDayCardsForNotDoneStatus(
    name: String,
    taskName: String,
    notDoneReason: DayCardReasonNotDoneEnumAvro?
) {
  submitDayCardG2(asReference = name, eventType = DayCardEventEnumAvro.CREATED) {
    it.status = DayCardStatusEnumAvro.OPEN
    it.task = getByReference(taskName)
  }
  submitDayCardG2(asReference = name, eventType = DayCardEventEnumAvro.COMPLETED) {
    it.status = DayCardStatusEnumAvro.DONE
    it.task = getByReference(taskName)
  }
  submitDayCardG2(asReference = name, eventType = DayCardEventEnumAvro.CANCELLED) {
    it.status = DayCardStatusEnumAvro.NOTDONE
    it.task = getByReference(taskName)
    it.reason = notDoneReason ?: DayCardReasonNotDoneEnumAvro.BAD_WEATHER
  }
}

private fun EventStreamGenerator.submitDayCardsForDoneStatus(name: String, taskName: String) {
  submitDayCardG2(asReference = name, eventType = DayCardEventEnumAvro.CREATED) {
    it.status = DayCardStatusEnumAvro.OPEN
    it.task = getByReference(taskName)
  }
  submitDayCardG2(asReference = name, eventType = DayCardEventEnumAvro.COMPLETED) {
    it.status = DayCardStatusEnumAvro.DONE
    it.task = getByReference(taskName)
  }
  submitDayCardG2(asReference = name, eventType = DayCardEventEnumAvro.RESET) {
    it.status = DayCardStatusEnumAvro.OPEN
    it.task = getByReference(taskName)
  }
  submitDayCardG2(asReference = name, eventType = DayCardEventEnumAvro.COMPLETED) {
    it.status = DayCardStatusEnumAvro.DONE
    it.task = getByReference(taskName)
  }
}

private fun EventStreamGenerator.submitDayCardsForApprovedStatus(name: String, taskName: String) {
  submitDayCardG2(asReference = name, eventType = DayCardEventEnumAvro.CREATED) {
    it.status = DayCardStatusEnumAvro.OPEN
    it.task = getByReference(taskName)
  }
  submitDayCardG2(asReference = name, eventType = DayCardEventEnumAvro.COMPLETED) {
    it.status = DayCardStatusEnumAvro.DONE
    it.task = getByReference(taskName)
  }
  submitDayCardG2(asReference = name, eventType = DayCardEventEnumAvro.APPROVED) {
    it.status = DayCardStatusEnumAvro.APPROVED
    it.task = getByReference(taskName)
  }
}

private fun EventStreamGenerator.submitDayCardG2UntilState(
    name: String = "dayCard",
    taskName: String = "task",
    dayCardStatus: DayCardStatusEnumAvro = DayCardStatusEnumAvro.OPEN,
    notDoneReason: DayCardReasonNotDoneEnumAvro?
): String {
  when (dayCardStatus) {
    DayCardStatusEnumAvro.OPEN -> submitDayCardForOpenStatus(name, taskName)
    DayCardStatusEnumAvro.NOTDONE -> submitDayCardsForNotDoneStatus(name, taskName, notDoneReason)
    DayCardStatusEnumAvro.DONE -> submitDayCardsForDoneStatus(name, taskName)
    DayCardStatusEnumAvro.APPROVED -> submitDayCardsForApprovedStatus(name, taskName)
    else -> error("Unknown day card event")
  }
  return name
}
