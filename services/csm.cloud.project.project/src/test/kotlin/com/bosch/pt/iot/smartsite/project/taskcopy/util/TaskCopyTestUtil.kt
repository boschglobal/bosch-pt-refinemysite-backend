/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskcopy.util

import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.taskcopy.shared.dto.OverridableTaskParametersDto
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import org.assertj.core.api.Assertions.assertThat

object TaskCopyTestUtil {

  fun assertCreatedTaskMatchCopiedTask(
      createdTask: Task,
      copiedTask: Task,
      parametersDto: OverridableTaskParametersDto? = null
  ) {
    assertThat(createdTask.name).isEqualTo(copiedTask.name)
    assertThat(createdTask.description).isEqualTo(copiedTask.description)
    assertThat(createdTask.location).isEqualTo(copiedTask.location)
    assertThat(createdTask.status).isEqualTo(TaskStatusEnum.DRAFT)
    assertThat(createdTask.project.identifier).isEqualTo(copiedTask.project.identifier)
    assertThat(createdTask.projectCraft.identifier).isEqualTo(copiedTask.projectCraft.identifier)
    assertThat(createdTask.assignee).isNull()
    if (parametersDto != null)
        assertThat(createdTask.workArea?.identifier).isEqualTo(parametersDto.workAreaId?.identifier)
  }

  fun assertCreatedTaskScheduleMatchCopiedTaskSchedule(
      createdTaskSchedule: TaskSchedule,
      copiedTaskSchedule: TaskSchedule,
      shiftDays: Long,
      includeDayCards: Boolean
  ) {
    assertThat(createdTaskSchedule.start).isEqualTo(copiedTaskSchedule.start?.plusDays(shiftDays))
    assertThat(createdTaskSchedule.end).isEqualTo(copiedTaskSchedule.end?.plusDays(shiftDays))
    if (includeDayCards) {
      createdTaskSchedule.slots?.forEachIndexed { index, createdTaskScheduleSlot ->
        assertThat(createdTaskScheduleSlot.date)
            .isEqualTo(copiedTaskSchedule.slots!![index].date.plusDays(shiftDays))
        assertCreatedDayCardMatchCopiedDayCard(
            createdTaskScheduleSlot.dayCard, copiedTaskSchedule.slots!![index].dayCard)
      }
    } else {
      assertThat(createdTaskSchedule.slots).isEmpty()
    }
  }

  private fun assertCreatedDayCardMatchCopiedDayCard(
      createdDayCard: DayCard,
      copiedDayCard: DayCard
  ) {
    assertThat(createdDayCard.title).isEqualTo(copiedDayCard.title)
    assertThat(createdDayCard.manpower).isEqualTo(copiedDayCard.manpower)
    assertThat(createdDayCard.notes).isEqualTo(copiedDayCard.notes)
    assertThat(createdDayCard.status).isEqualTo(DayCardStatusEnum.OPEN)
    assertThat(createdDayCard.reason).isNull()
  }
}
