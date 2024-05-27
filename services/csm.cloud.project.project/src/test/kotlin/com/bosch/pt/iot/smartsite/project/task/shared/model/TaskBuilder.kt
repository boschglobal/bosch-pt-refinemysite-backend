/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.shared.model

import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.project.shared.model.ParticipantBuilder.Companion.participant
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectBuilder.Companion.project
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraftBuilder.Companion.projectCraft
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import com.bosch.pt.iot.smartsite.user.model.User
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.LinkedList

class TaskBuilder private constructor() {

  private var name: String? = null
  private var location: String? = null
  private var projectCraft: ProjectCraft? = null
  private var workArea: WorkArea? = null
  private var assignee: Participant? = null
  private var status: TaskStatusEnum? = null
  private var project: Project? = null
  private var description: String? = null
  private var createdDate = now()
  private var lastModifiedDate = now()
  private var createdBy: User? = null
  private var lastModifiedBy: User? = null
  private var identifier: TaskId? = null
  private val topics: MutableList<Topic> = LinkedList()
  private var taskSchedule: TaskSchedule? = null

  fun withName(name: String?): TaskBuilder {
    this.name = name
    return this
  }

  fun withLocation(location: String?): TaskBuilder {
    this.location = location
    return this
  }

  fun withProjectCraft(projectCraft: ProjectCraft?): TaskBuilder {
    this.projectCraft = projectCraft
    return this
  }

  fun withWorkArea(workArea: WorkArea?): TaskBuilder {
    this.workArea = workArea
    return this
  }

  fun withAssignee(assignee: Participant?): TaskBuilder {
    this.assignee = assignee
    return this
  }

  fun withoutAssignee(): TaskBuilder {
    assignee = null
    return this
  }

  fun withStatus(status: TaskStatusEnum?): TaskBuilder {
    this.status = status
    return this
  }

  fun withProject(project: Project?): TaskBuilder {
    this.project = project
    return this
  }

  fun withDescription(description: String?): TaskBuilder {
    this.description = description
    return this
  }

  fun withCreatedDate(createdDate: LocalDateTime?): TaskBuilder {
    this.createdDate = createdDate
    return this
  }

  fun withLastModifiedDate(lastModifiedDate: LocalDateTime?): TaskBuilder {
    this.lastModifiedDate = lastModifiedDate
    return this
  }

  fun withCreatedBy(createdBy: User?): TaskBuilder {
    this.createdBy = createdBy
    return this
  }

  fun withLastModifiedBy(lastModifiedBy: User?): TaskBuilder {
    this.lastModifiedBy = lastModifiedBy
    return this
  }

  fun withIdentifier(identifier: TaskId?): TaskBuilder {
    this.identifier = identifier
    return this
  }

  fun withTopic(topic: Topic): TaskBuilder {
    topics.add(topic)
    return this
  }

  fun withTopics(topics: List<Topic>?): TaskBuilder {
    this.topics.addAll(topics!!)
    return this
  }

  fun withTaskSchedule(taskSchedule: TaskSchedule?): TaskBuilder {
    this.taskSchedule = taskSchedule
    return this
  }

  fun build(): Task {
    val task =
        Task(
            identifier ?: TaskId(),
            project!!,
            name!!,
            description,
            location,
            projectCraft!!,
            status!!,
            assignee,
            workArea)
    if (createdDate != null) {
      task.setCreatedDate(createdDate)
    }
    createdBy?.getAuditUserId()?.let { task.setCreatedBy(it) }
    createdBy?.getAuditUserId()?.let { task.setLastModifiedBy(it) }
    if (lastModifiedDate != null) {
      task.setLastModifiedDate(lastModifiedDate)
    }
    task.addTopic(topics)
    task.taskSchedule = taskSchedule

    return task
  }

  companion object {

    @JvmStatic
    fun task(): TaskBuilder =
        TaskBuilder()
            .withStatus(OPEN)
            .withName("task1")
            .withIdentifier(TaskId())
            .withAssignee(participant().build())
            .withDescription("description")
            .withLocation("location")
            .withProjectCraft(projectCraft().build())
            .withProject(project().build())
  }
}
