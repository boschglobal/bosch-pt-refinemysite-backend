/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskschedule.shared.model

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.time.LocalDate
import org.apache.commons.lang3.builder.ToStringBuilder

@Entity
@Table(
    indexes =
        [
            Index(name = "UK_TaskSchedule_Identifier", columnList = "identifier", unique = true),
            Index(name = "UK_TaskSchedule_TaskIdentifier", columnList = "task_id", unique = true)])
class TaskSchedule : AbstractSnapshotEntity<Long, TaskScheduleId> {

  // Note: this field has been introduced to optimize database query performance. It is
  // intentionally not part of the Avro message.
  @JoinColumn(foreignKey = ForeignKey(name = "FK_TaskSchedule_Project"), nullable = false)
  @ManyToOne(fetch = LAZY)
  lateinit var project: Project

  /** Associated task. */
  @OneToOne(fetch = LAZY, optional = false)
  @JoinColumn(foreignKey = ForeignKey(name = "FK_TaskSchedule_Task"))
  lateinit var task: Task

  @Column(name = "start_date") var start: LocalDate? = null

  @Column(name = "end_date") var end: LocalDate? = null

  @OneToMany
  @JoinColumn(
      name = "taskschedule_id",
      foreignKey = ForeignKey(name = "FK_TaskSchedule_TaskScheduleSlot_TaskScheduleId"))
  @OrderBy("day_card_date ASC")
  var slots: MutableList<TaskScheduleSlot>? = null

  constructor() {
    // empty
  }

  constructor(task: Task, start: LocalDate?, end: LocalDate?) {
    this.identifier = TaskScheduleId()
    this.project = task.project
    this.task = task
    this.start = start
    this.end = end
    this.slots = ArrayList()
  }

  fun getDayCard(slotDate: LocalDate): DayCard? =
      requireNotNull(slots).filter { it.date == slotDate }.map { it.dayCard }.firstOrNull()

  fun getSlot(slotDate: LocalDate): TaskScheduleSlot? =
      requireNotNull(slots).firstOrNull { it.date == slotDate }

  override fun getDisplayName(): String =
      ToStringBuilder(this)
          .appendSuper(super.toString())
          .append("start", start)
          .append("end", end)
          .toString()

  companion object {
    private const val serialVersionUID: Long = 1988764601202394104
  }
}
