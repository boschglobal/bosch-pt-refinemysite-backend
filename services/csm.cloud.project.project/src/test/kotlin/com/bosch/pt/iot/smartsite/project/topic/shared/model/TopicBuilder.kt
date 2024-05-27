/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.shared.model

import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskBuilder.Companion.task
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import com.bosch.pt.iot.smartsite.user.model.User
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID

@Deprecated("To be removed with the cleanup of the old test data creation")
class TopicBuilder private constructor() {

  private var criticality: TopicCriticalityEnum? = null
  private var task: Task? = null
  private var description: String? = null
  private var identifier: UUID? = null
  private var createdBy: User? = null
  private var lastModifiedBy: User? = null
  private var createdDate = now()
  private var lastModifiedDate = now()

  fun withCriticality(criticality: TopicCriticalityEnum?): TopicBuilder = apply {
    this.criticality = criticality
  }

  fun withTask(task: Task?): TopicBuilder = apply { this.task = task }

  fun withDescription(description: String?): TopicBuilder = apply { this.description = description }

  fun withCreatedBy(user: User?): TopicBuilder = apply { this.createdBy = user }

  fun withIdentifier(identifier: UUID?): TopicBuilder = apply { this.identifier = identifier }

  fun withLastModifiedBy(lastModifiedBy: User?): TopicBuilder = apply {
    this.lastModifiedBy = lastModifiedBy
  }

  fun withCreatedDate(createdDate: LocalDateTime): TopicBuilder = apply {
    this.createdDate = createdDate
  }

  fun withLastModifiedDate(lastModifiedDate: LocalDateTime): TopicBuilder = apply {
    this.lastModifiedDate = lastModifiedDate
  }

  fun build(): Topic {
    val topic = Topic(identifier!!.asTopicId(), criticality!!, description, task!!, mutableSetOf())
    topic.setCreatedDate(createdDate)
    topic.setLastModifiedDate(lastModifiedDate)
    createdBy?.getAuditUserId()?.let { topic.setCreatedBy(it) }
    createdBy?.getAuditUserId()?.let { topic.setLastModifiedBy(it) }
    return topic
  }

  companion object {

    @JvmStatic
    fun topic(): TopicBuilder =
        TopicBuilder()
            .withIdentifier(UUID.randomUUID())
            .withCriticality(TopicCriticalityEnum.UNCRITICAL)
            .withDescription("description")
            .withTask(task().build())
  }
}
