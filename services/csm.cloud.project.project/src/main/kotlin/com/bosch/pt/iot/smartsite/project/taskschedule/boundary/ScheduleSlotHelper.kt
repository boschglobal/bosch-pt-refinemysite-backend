/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.boundary

import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import java.time.DayOfWeek
import java.time.LocalDate

object ScheduleSlotHelper {

  fun shiftDayCardOutFromSlot(
      slotToFree: LocalDate,
      scheduleSlots: MutableMap<DayCardId, LocalDate>,
      workdayConfiguration: WorkdayConfiguration
  ): MutableMap<DayCardId, LocalDate> {

    // Validate that the slot is occupied ( shift needed )
    val dayCardToInsert = scheduleSlots.entries.firstOrNull { slotToFree == it.value }?.key
    checkNotNull(dayCardToInsert) {
      "An unexpected error occurred while trying to get the first day card to shifting."
    }

    // Set up the fields used by the iteration
    var iteratorSlot = slotToFree.plusDays(1)
    val queueOfDayCardsToInsertInNextFreeSlot = mutableListOf(dayCardToInsert)

    // Set up all the information of the working configuration needed
    val allowWorkOnNonWorkingDays = workdayConfiguration.allowWorkOnNonWorkingDays
    val workdays = workdayConfiguration.workingDays
    val holidays = workdayConfiguration.holidays.map { it.date }.toSet()

    do {

      // Get all needed information from the slot to free
      val dayCardOfSlot = scheduleSlots.entries.firstOrNull { iteratorSlot == it.value }?.key
      val isOccupiedSlot = dayCardOfSlot != null
      val isNonWorkingDay =
          validateNonWorkingDay(iteratorSlot, workdays, holidays, allowWorkOnNonWorkingDays)

      /*
       * Validate the three possible scenarios:
       *
       * 1) The insert date is a non-working day and slot contains a day card.
       * In this case we need to queue the occupied slot day card to process it later.
       *
       * 2) The insert date is a working day and slot contains a day card.
       * In this case we need to queue the occupied slot day card to process it later.
       * The first queue day card to process is removed
       * and will occupy the slot of the insert date in the shift result
       *
       * 3) The insert date is a working day and slot is free.
       * The first queue day card to process is removed
       * and will occupy the slot of the insert date in the shift result
       *
       * If none of the above scenarios are possible it is because is a non-working day
       * and the iteration moves on (no action needed)
       */
      when {
        isNonWorkingDay && isOccupiedSlot -> {
          requireNotNull(dayCardOfSlot)
          queueOfDayCardsToInsertInNextFreeSlot.add(dayCardOfSlot)
        }
        isOccupiedSlot -> {
          requireNotNull(dayCardOfSlot)
          queueOfDayCardsToInsertInNextFreeSlot.add(dayCardOfSlot)

          scheduleSlots[queueOfDayCardsToInsertInNextFreeSlot.removeFirst()] = iteratorSlot
        }
        !isNonWorkingDay && !isOccupiedSlot -> {
          scheduleSlots[queueOfDayCardsToInsertInNextFreeSlot.removeFirst()] = iteratorSlot
        }
      }

      // Iterate to the next slot ( the next day )
      iteratorSlot = iteratorSlot.plusDays(1)
    } while (queueOfDayCardsToInsertInNextFreeSlot.isNotEmpty())

    return scheduleSlots
  }

  private fun validateNonWorkingDay(
      validationDate: LocalDate,
      workingDays: Set<DayOfWeek>,
      holidays: Set<LocalDate>,
      allowWorkOnNonWorkingDays: Boolean
  ) =
      !allowWorkOnNonWorkingDays &&
          (!workingDays.contains(validationDate.dayOfWeek) || holidays.contains(validationDate))
}
