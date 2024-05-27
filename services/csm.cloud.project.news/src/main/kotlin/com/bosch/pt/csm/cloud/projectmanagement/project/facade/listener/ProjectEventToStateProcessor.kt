/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.project.facade.listener

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.news.boundary.NewsService
import com.bosch.pt.csm.cloud.projectmanagement.news.boundary.ObjectRelationService
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.boundary.ParticipantMappingService
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventG2Avro
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

/**
 * This class is responsible for updating the local state based on a project event. The local state
 * contains the view of the event log required to determine news.
 */
@Component
class ProjectEventToStateProcessor(
    private val objectRelationService: ObjectRelationService,
    private val participantMappingService: ParticipantMappingService,
    private val newsService: NewsService
) {

  fun process(value: SpecificRecordBase?) {
    if (value is TaskEventAvro && value.name == TaskEventEnumAvro.DELETED) {
      deleteByTaskEvent(value)
    } else if (value is TopicEventG2Avro && value.name == TopicEventEnumAvro.DELETED) {
      deleteByTopicEvent(value)
    } else if (value is TaskAttachmentEventAvro &&
        value.name == TaskAttachmentEventEnumAvro.DELETED) {
      deleteByTaskAttachmentEvent(value)
    } else if (value is ProjectEventAvro && value.name == ProjectEventEnumAvro.DELETED) {
      deleteByProjectEvent(value)
    } else if (value is MessageEventAvro && value.name == MessageEventEnumAvro.DELETED) {
      deleteByMessageEvent(value)
    } else if (value is ParticipantEventG3Avro &&
        value.name == ParticipantEventEnumAvro.DEACTIVATED) {
      deleteByParticipantEvent(value)
    } else {
      updateStateFromProjectEvent(value)
    }
  }

  private fun deleteByProjectEvent(projectEvent: ProjectEventAvro) {
    val projectIdentifier = projectEvent.aggregate.aggregateIdentifier
    val taskIdentifiers = objectRelationService.findTaskIdentifiers(projectIdentifier)

    objectRelationService.deleteByProjectIdentifier(projectIdentifier)
    newsService.deleteByTaskIdentifiers(taskIdentifiers)
    participantMappingService.deleteByProjectIdentifier(projectIdentifier)
  }

  private fun deleteByTaskAttachmentEvent(taskAttachmentEvent: TaskAttachmentEventAvro) {
    val taskAttachmentIdentifier = taskAttachmentEvent.aggregate.aggregateIdentifier
    objectRelationService.deleteByTaskAttachmentIdentifier(taskAttachmentIdentifier)
  }

  private fun deleteByTaskEvent(taskEvent: TaskEventAvro) {
    val taskIdentifier = taskEvent.aggregate.aggregateIdentifier
    newsService.deleteByTaskIdentifier(taskIdentifier)
    objectRelationService.deleteByTaskIdentifier(taskIdentifier)
  }

  private fun deleteByTopicEvent(topicEvent: TopicEventG2Avro) {
    val topicIdentifier = topicEvent.aggregate.aggregateIdentifier
    objectRelationService.deleteByTopicIdentifier(topicIdentifier)
  }

  private fun deleteByMessageEvent(messageEventAvro: MessageEventAvro) {
    val messageIdentifier = messageEventAvro.aggregate.aggregateIdentifier
    newsService.deleteByMessageIdentifier(messageIdentifier)
    objectRelationService.deleteByMessageIdentifier(messageIdentifier)
  }

  private fun deleteByParticipantEvent(participantEventAvro: ParticipantEventG3Avro) {
    val userIdentifier = participantEventAvro.aggregate.user.identifier.toUUID()
    val projectIdentifier = participantEventAvro.aggregate.project
    val taskIdentifiers = objectRelationService.findTaskIdentifiers(projectIdentifier)

    newsService.deleteAllByUserIdentifierAndRootObjectIn(userIdentifier, taskIdentifiers)
    participantMappingService.deleteByProjectIdentifierAndUserIdentifier(
        participantEventAvro.aggregate.project.identifier.toUUID(), userIdentifier)
  }

  private fun updateStateFromProjectEvent(message: SpecificRecordBase?) {
    if ((message is ParticipantEventG3Avro && message.name != ParticipantEventEnumAvro.CANCELLED) &&
        message.aggregate.status != ParticipantStatusEnumAvro.VALIDATION &&
        message.aggregate.status != ParticipantStatusEnumAvro.INVITED) {

      updateFromParticipantEvent(message)
    } else if (message is TaskEventAvro) {
      updateFromTaskEvent(message)
    } else if (message is TaskScheduleEventAvro) {
      updateFromTaskScheduleEvent(message)
    } else if (message is TaskAttachmentEventAvro) {
      updateFromTaskAttachmentEvent(message)
    } else if (message is TopicEventG2Avro) {
      updateFromTopicEvent(message)
    } else if (message is TopicAttachmentEventAvro) {
      updateFromTopicAttachmentEvent(message)
    } else if (message is MessageEventAvro) {
      updateFromMessageEvent(message)
    } else if (message is MessageAttachmentEventAvro) {
      updateFromMessageAttachmentEvent(message)
    }
  }

  private fun updateFromParticipantEvent(event: ParticipantEventG3Avro) {
    objectRelationService.saveParticipantToUserRelation(event)
    objectRelationService.saveParticipantToCompanyRelation(event)
    objectRelationService.saveParticipantToProjectRelation(event)

    participantMappingService.saveParticipant(event)
  }

  private fun updateFromTaskEvent(event: TaskEventAvro) {
    if (event.name == TaskEventEnumAvro.UNASSIGNED) {
      objectRelationService.deleteTaskToAssignedUserRelation(event)
      objectRelationService.deleteTaskToAssignedCompanyRelation(event)
    } else if (event.aggregate.assignee != null) {
      objectRelationService.saveTaskToAssignedUserRelation(event)
      objectRelationService.saveTaskToAssignedCompanyRelation(event)
    }

    objectRelationService.saveTaskToProjectRelation(event)
  }

  private fun updateFromTaskScheduleEvent(event: TaskScheduleEventAvro) =
      objectRelationService.saveTaskScheduleToTaskRelation(event)

  private fun updateFromTaskAttachmentEvent(event: TaskAttachmentEventAvro) =
      objectRelationService.saveTaskAttachmentToTaskRelation(event)

  private fun updateFromTopicEvent(event: TopicEventG2Avro) =
      objectRelationService.saveTopicToTaskRelation(event)

  private fun updateFromTopicAttachmentEvent(event: TopicAttachmentEventAvro) =
      objectRelationService.saveTopicAttachmentToTopicRelation(event)

  private fun updateFromMessageEvent(event: MessageEventAvro) =
      objectRelationService.saveMessageToTopicRelation(event)

  private fun updateFromMessageAttachmentEvent(event: MessageAttachmentEventAvro) =
      objectRelationService.saveMessageAttachmentToMessageRelation(event)
}
