/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskschedule.shared.model

import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskBuilder.Companion.task
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import com.bosch.pt.iot.smartsite.user.model.User
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalDateTime.now

class TaskScheduleBuilder private constructor() {
  private var task: Task? = null
  private var start: LocalDate? = null
  private var end: LocalDate? = null
  private var slots: List<TaskScheduleSlot>? = ArrayList()
  private var createdDate = now()
  private var lastModifiedDate = now()
  private var createdBy: User? = null
  private var lastModifiedBy: User? = null
  private var identifier: TaskScheduleId? = null
  fun withTask(task: Task?): TaskScheduleBuilder {
    this.task = task
    return this
  }
  fun withStart(start: LocalDate?): TaskScheduleBuilder = apply { this.start = start }
  fun withEnd(end: LocalDate?): TaskScheduleBuilder = apply { this.end = end }
  fun withSlots(slots: List<TaskScheduleSlot>?): TaskScheduleBuilder = apply { this.slots = slots }
  fun withCreatedDate(createdDate: LocalDateTime?): TaskScheduleBuilder = apply {
    this.createdDate = createdDate
  }
  fun withLastModifiedDate(lastModifiedDate: LocalDateTime?): TaskScheduleBuilder = apply {
    this.lastModifiedDate = lastModifiedDate
  }
  fun withCreatedBy(createdBy: User?): TaskScheduleBuilder = apply { this.createdBy = createdBy }
  fun withLastModifiedBy(lastModifiedBy: User?): TaskScheduleBuilder = apply {
    this.lastModifiedBy = lastModifiedBy
  }
  fun withIdentifier(identifier: TaskScheduleId?): TaskScheduleBuilder = apply {
    this.identifier = identifier
  }
  fun build(): TaskSchedule {
    val taskSchedule = TaskSchedule()

    taskSchedule.project = task?.project!!
    taskSchedule.task = task!!
    taskSchedule.start = start
    taskSchedule.end = end
    taskSchedule.slots = slots?.toMutableList()

    if (createdDate != null) {
      taskSchedule.setCreatedDate(createdDate)
    }
    if (lastModifiedDate != null) {
      taskSchedule.setLastModifiedDate(lastModifiedDate)
    }
    createdBy?.getAuditUserId()?.let { taskSchedule.setCreatedBy(it) }
    lastModifiedBy?.getAuditUserId()?.let { taskSchedule.setLastModifiedBy(it) }

    taskSchedule.identifier = identifier!!
    return taskSchedule
  }
  companion object {
    @JvmStatic
    fun taskSchedule(): TaskScheduleBuilder =
        TaskScheduleBuilder().withIdentifier(TaskScheduleId()).withTask(task().build())
  }
}
