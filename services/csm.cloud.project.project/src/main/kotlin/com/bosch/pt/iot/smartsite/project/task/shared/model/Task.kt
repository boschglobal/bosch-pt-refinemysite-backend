/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.shared.model

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import jakarta.persistence.CascadeType.PERSIST
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.LinkedList

@Entity
@Table(indexes = [Index(name = "UK_Task_Identifier", columnList = "identifier", unique = true)])
class Task : AbstractSnapshotEntity<Long, TaskId> {

  @JoinColumn(foreignKey = ForeignKey(name = "FK_Task_Project"))
  @ManyToOne(fetch = LAZY, optional = false)
  lateinit var project: Project

  @field:Size(min = 1, max = MAX_NAME_LENGTH)
  @Column(nullable = false, length = MAX_NAME_LENGTH)
  lateinit var name: String

  @field:Size(max = MAX_DESCRIPTION_LENGTH)
  @Column(length = MAX_DESCRIPTION_LENGTH)
  var description: String? = null

  @field:Size(max = MAX_LOCATION_LENGTH)
  @Column(length = MAX_LOCATION_LENGTH)
  var location: String? = null

  @JoinColumn(foreignKey = ForeignKey(name = "FK_Task_Craft"))
  @ManyToOne(fetch = LAZY, optional = false)
  lateinit var projectCraft: ProjectCraft

  @JoinColumn(foreignKey = ForeignKey(name = "FK_Task_Assignee"))
  @ManyToOne(fetch = LAZY)
  var assignee: Participant? = null

  @JoinColumn(foreignKey = ForeignKey(name = "FK_Task_WorkArea"))
  @ManyToOne(fetch = LAZY)
  var workArea: WorkArea? = null

  @Column(nullable = false) lateinit var status: TaskStatusEnum

  @OneToMany(fetch = LAZY, mappedBy = "task", cascade = [PERSIST])
  var topics: MutableList<Topic>? = null

  /** Date when user last edited description, headline or status. */
  var editDate: LocalDateTime? = null

  @OneToOne(fetch = LAZY, mappedBy = "task") var taskSchedule: TaskSchedule? = null

  @Column(columnDefinition = "bit not null default 0", insertable = false) var deleted = false

  constructor() {
    // empty
  }

  constructor(
      identifier: TaskId,
      project: Project,
      name: String,
      description: String?,
      location: String?,
      projectCraft: ProjectCraft,
      status: TaskStatusEnum,
      assignee: Participant? = null,
      workArea: WorkArea? = null
  ) {
    this.identifier = identifier
    this.project = project
    this.name = name
    this.description = description
    this.location = location
    this.projectCraft = projectCraft
    this.assignee = assignee
    this.workArea = workArea
    this.status = status
  }

  fun addTopic(topics: List<Topic>) {
    if (this.topics == null) {
      this.topics = LinkedList()
    }

    topics.forEach { topic: Topic ->
      this.topics!!.add(topic)
      topic.task = this
    }
  }

  fun isAssigned(): Boolean = assignee != null

  override fun getDisplayName(): String = name

  companion object {
    private const val serialVersionUID: Long = -3248416769510934024

    const val MAX_NAME_LENGTH = 100
    const val MAX_DESCRIPTION_LENGTH = 1000
    const val MAX_LOCATION_LENGTH = 100
  }
}
