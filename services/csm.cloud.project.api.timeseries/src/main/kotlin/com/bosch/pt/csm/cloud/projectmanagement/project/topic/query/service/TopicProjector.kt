/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.service

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.asTaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.domain.asTopicId
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.model.Topic
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.model.TopicCriticalityEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.model.TopicMapper
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.model.TopicVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.repository.TopicRepository
import com.bosch.pt.csm.cloud.projectmanagement.topic.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class TopicProjector(private val repository: TopicRepository) {

  fun onTopicEvent(aggregate: TopicAggregateG2Avro, projectId: ProjectId) {
    val existingTopic = repository.findOneByIdentifier(aggregate.getIdentifier().asTopicId())

    if (existingTopic == null || aggregate.aggregateIdentifier.version > existingTopic.version) {
      (existingTopic?.updateFromTopicAggregate(aggregate) ?: aggregate.toNewProjection(projectId))
          .apply { repository.save(this) }
    }
  }

  fun onTopicDeletedEvent(aggregate: TopicAggregateG2Avro) {
    val topic = repository.findOneByIdentifier(aggregate.getIdentifier().asTopicId())
    if (topic != null && !topic.deleted) {
      val newVersion =
          topic.history
              .last()
              .copy(
                  deleted = true,
                  version = aggregate.aggregateIdentifier.version,
                  eventDate =
                      aggregate.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis(),
                  eventAuthor =
                      aggregate.auditingInformation.lastModifiedBy.identifier.toUUID().asUserId())

      repository.save(
          TopicMapper.INSTANCE.fromTopicVersion(
              newVersion,
              topic.identifier,
              topic.project,
              topic.task,
              topic.history.toMutableList().also { it.add(newVersion) }))
    }
  }

  private fun TopicAggregateG2Avro.toNewProjection(projectId: ProjectId): Topic {
    val topicVersion = this.newTopicVersion()

    return TopicMapper.INSTANCE.fromTopicVersion(
        topicVersion = topicVersion,
        identifier = aggregateIdentifier.identifier.toUUID().asTopicId(),
        project = projectId,
        task = task.identifier.toUUID().asTaskId(),
        history = listOf(topicVersion))
  }

  private fun Topic.updateFromTopicAggregate(aggregate: TopicAggregateG2Avro): Topic {
    val topicVersion = aggregate.newTopicVersion()

    return TopicMapper.INSTANCE.fromTopicVersion(
        topicVersion = topicVersion,
        identifier = this.identifier,
        project = this.project,
        task = this.task,
        history = this.history.toMutableList().also { it.add(topicVersion) })
  }

  private fun TopicAggregateG2Avro.newTopicVersion(): TopicVersion {
    val isNew = this.aggregateIdentifier.version == 0L
    val auditUser: UserId
    val auditDate: LocalDateTime
    if (isNew) {
      auditUser = UserId(this.auditingInformation.createdBy.identifier.toUUID())
      auditDate = this.auditingInformation.createdDate.toLocalDateTimeByMillis()
    } else {
      auditUser = UserId(this.auditingInformation.lastModifiedBy.identifier.toUUID())
      auditDate = this.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis()
    }

    return TopicVersion(
        version = this.aggregateIdentifier.version,
        criticality = TopicCriticalityEnum.valueOf(this.criticality.name),
        description = this.description,
        eventAuthor = auditUser,
        eventDate = auditDate)
  }
}
