/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.domain.DayCardId
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.rest.resource.response.DayCardResource
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCard
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.model.TaskSchedule
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class DayCardResourceAssembler {

  fun assembleLatest(dayCard: DayCard, taskSchedule: TaskSchedule?): DayCardResource? {
    if (taskSchedule == null) {
      return null
    }
    val cardVersion = dayCard.history.last()
    val versionDate =
        findLatestVersionDates(taskSchedule, dayCard.identifier, cardVersion.version).maxByOrNull {
          it.second
        }
            ?: return null

    val dayCardDate =
        if (versionDate.second.isAfter(cardVersion.eventDate)) versionDate.second
        else cardVersion.eventDate

    return DayCardResourceMapper.INSTANCE.fromDayCardVersion(
        cardVersion,
        dayCard.project,
        dayCard.task,
        dayCard.identifier,
        versionDate.first,
        cardVersion.reason?.key,
        dayCardDate)
  }

  fun assemble(
      dayCard: DayCard,
      taskSchedule: TaskSchedule?,
  ): List<DayCardResource> {
    if (taskSchedule == null) {
      return emptyList()
    }

    val versionDates =
        dayCard.history.associate { card ->
          card.version to findVersionDates(taskSchedule, dayCard.identifier, card.version)
        }

    val projectId = dayCard.project

    return dayCard.history
        .flatMap { cardVersion ->
          val changes = mutableListOf<DayCardResource>()

          // Each new version implies that the day card was updated (content wise)
          // Find the last scheduled date of a previous day card version.
          // The first version can be ignored as it is added below with the first
          // schedule version.
          if (cardVersion.version > 0L) {
            val date =
                lastScheduledDateOfDayCard(
                    dayCard.identifier, cardVersion.version - 1, taskSchedule)

            // Ignore errors if the day card cannot be found in the schedule
            if (date != null) {
              changes.add(
                  DayCardResourceMapper.INSTANCE.fromDayCardVersion(
                      cardVersion,
                      projectId,
                      dayCard.task,
                      dayCard.identifier,
                      date,
                      cardVersion.reason?.key,
                      cardVersion.eventDate))
            }
          }

          // If there are updated schedules for a day card version, the changes must be represented
          // by additional rows with the changed dates from the schedule.
          val dates = versionDates[cardVersion.version] ?: emptyList()

          dates.map { scheduleUpdate ->
            // Return a row for each date change even if the version didn't change
            // Take the event date from the schedule update
            val scheduleDate = scheduleUpdate.first
            val scheduleEventDate = scheduleUpdate.second
            val eventDate =
                if (scheduleEventDate.isAfter(cardVersion.eventDate)) scheduleEventDate
                else cardVersion.eventDate

            changes.add(
                DayCardResourceMapper.INSTANCE.fromDayCardVersion(
                    cardVersion,
                    projectId,
                    dayCard.task,
                    dayCard.identifier,
                    scheduleDate,
                    cardVersion.reason?.key,
                    eventDate))
          }

          // There can be the same day card multiple times in the schedule without a change of the
          // day or any content change (e.g. if subsequently day cards are added to the schedule).
          // Filter duplicated out of the result.
          changes.distinctBy { getAttributesWithoutEventTimestamp(it) }
        }
        // Deduplicate for confidence reason - to filter out potential duplicates.
        // Shouldn't be required if no duplicates exist in the data.
        .distinct()
  }

  /**
   * Find for each day card version the dates in the schedule. There can be zero, one or more than
   * one date for a card version:
   *
   * Zero - Day card was updated (content wise)
   *
   * One - Day card was added to schedule or date was changed
   *
   * &#62 One - Day card was rescheduled multiple times
   */
  private fun findVersionDates(taskSchedule: TaskSchedule, identifier: DayCardId, version: Long) =
      taskSchedule.history.flatMap { scheduleVersion ->
        scheduleVersion.slots
            .filter { it.dayCardId == identifier && it.dayCardVersion == version }
            .map { Pair(it.date, scheduleVersion.eventDate) }
            .distinct()
      }

  private fun findLatestVersionDates(
      taskSchedule: TaskSchedule,
      identifier: DayCardId,
      version: Long
  ): List<Pair<LocalDate, LocalDateTime>> =
      if (version >= 0) {
        val versionDates = findVersionDates(taskSchedule, identifier, version)
        versionDates.ifEmpty { findLatestVersionDates(taskSchedule, identifier, version - 1) }
      } else emptyList()

  private fun lastScheduledDateOfDayCard(
      dayCardId: DayCardId,
      version: Long,
      taskSchedule: TaskSchedule
  ): LocalDate? =
      if (version >= 0) {
        taskSchedule.history
            .reversed()
            .firstOrNull { schedule ->
              schedule.slots.firstOrNull { slot ->
                slot.dayCardId == dayCardId && slot.dayCardVersion == version
              } != null
            }
            ?.slots
            ?.first { it.dayCardId == dayCardId }
            ?.date
            ?: lastScheduledDateOfDayCard(dayCardId, version - 1, taskSchedule)
      } else null

  private fun getAttributesWithoutEventTimestamp(dayCard: DayCardResource) =
      dayCard.javaClass.declaredFields.mapNotNull {
        if (it.name != "eventTimestamp") {
          if (!it.canAccess(dayCard)) {
            it.trySetAccessible()
          }
          return@mapNotNull it.get(dayCard)
        }
        return@mapNotNull null
      }
}
