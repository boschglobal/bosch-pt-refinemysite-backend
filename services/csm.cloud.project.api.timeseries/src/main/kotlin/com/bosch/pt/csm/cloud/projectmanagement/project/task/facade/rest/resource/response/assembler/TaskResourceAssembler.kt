/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.rest.resource.response.TaskResource
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.model.Task
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.model.TaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.model.TaskScheduleVersion
import org.springframework.stereotype.Component

@Component
class TaskResourceAssembler {

  fun assembleLatest(task: Task, schedules: List<TaskSchedule>): List<TaskResource> =
      task.history.last().let { taskVersion ->
        val latestSchedule = schedules.versions().sorted().lastOrNull()
        val taskEventDate = listOfNotNull(latestSchedule?.eventDate, task.eventDate).max()
        listOf(
            TaskResourceMapper.INSTANCE.fromTaskVersion(
                taskVersion = taskVersion,
                project = task.project,
                identifier = task.identifier,
                start = latestSchedule?.start,
                end = latestSchedule?.end,
                eventDate = taskEventDate))
      }

  fun assemble(task: Task, schedules: List<TaskSchedule>): List<TaskResource> {

    val changes = mutableListOf<TaskResource>()
    task.history.forEach { taskVersion ->
      // Return a task with the latest known date (from previous schedule if exists)
      val latestScheduleBeforeTaskVersion =
          schedules.versions().filter { it.taskVersion < taskVersion.version }.sorted().lastOrNull()
      changes.add(
          TaskResourceMapper.INSTANCE.fromTaskVersion(
              taskVersion = taskVersion,
              project = task.project,
              identifier = task.identifier,
              start = latestScheduleBeforeTaskVersion?.start,
              end = latestScheduleBeforeTaskVersion?.end,
              eventDate = taskVersion.eventDate))

      // If there's one or more updated schedule(s) that reference the currently
      // processed task version, then return a new task entry for each version of the schedule
      changes.addAll(
          schedules
              .versions()
              .filter { it.taskVersion == taskVersion.version }
              .sorted()
              .map {
                val start = if (!it.deleted) it.start else null
                val end = if (!it.deleted) it.end else null
                TaskResourceMapper.INSTANCE.fromTaskVersion(
                    taskVersion, task.project, task.identifier, start, end, it.eventDate)
              })
    }

    return changes.withoutDuplicates()
  }
}

fun List<TaskSchedule>.versions() = this.map { it.history }.flatten()

fun List<TaskScheduleVersion>.sorted(): List<TaskScheduleVersion> =
    this.sortedWith(
        compareBy(
            { it.taskVersion },
            {
              // ideally, we would use the schedule version here, but a schedule can any time be
              // deleted and recreated again. At recreation, the schedule version starts again
              // at 0. Only the event date establishes the correct event order, assuming the event
              // date is monotonically increasing.
              it.eventDate
            },
            { it.deleted }))

fun List<TaskResource>.withoutDuplicates() =
    (listOf(this.firstOrNull()) +
            this.zipWithNext { first, second -> if (second.isDuplicateOf(first)) null else second })
        .filterNotNull()

fun TaskResource.isDuplicateOf(other: TaskResource) =
    this.version == other.version && this.start == other.start && this.end == other.end
