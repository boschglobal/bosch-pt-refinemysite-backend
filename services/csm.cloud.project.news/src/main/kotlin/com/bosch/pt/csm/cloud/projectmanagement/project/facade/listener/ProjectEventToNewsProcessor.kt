/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.project.facade.listener

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.AbstractEventProcessor
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.news.boundary.NewsService
import com.bosch.pt.csm.cloud.projectmanagement.news.boundary.ObjectRelationService
import com.bosch.pt.csm.cloud.projectmanagement.news.model.News
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CR
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.projectmanagement.project.model.ParticipantMapping
import com.bosch.pt.csm.cloud.projectmanagement.project.repository.ParticipantMappingRepository
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventG2Avro
import datadog.trace.api.Trace
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** This class determines the news and all recipients for a project event. */
@Component
class ProjectEventToNewsProcessor(
    private val participantMappingRepository: ParticipantMappingRepository,
    private val objectRelationService: ObjectRelationService,
    private val newsService: NewsService
) : AbstractEventProcessor() {

  @Trace
  @Transactional(readOnly = true)
  override fun process(
      key: EventMessageKey,
      value: SpecificRecordBase?,
      recordTimestamp: LocalDateTime
  ) {
    // no need to create news for tombstone messages
    if (value == null) return

    if (!shouldProcessStateOnly(recordTimestamp)) {
      val (newsList, recipients) = createNewsFromEvent(value)
      newsService.updateNews(newsList, recipients)
    }
  }

  @Trace
  private fun createNewsFromEvent(event: SpecificRecordBase): Pair<List<News>, Set<UUID>> =
      determineNews(event).let { newsList ->
        if (newsList.isNotEmpty()) {
          Pair(
              newsList,
              determineRecipients(
                  newsList.first().rootObject, getLastModifiedByUserFromEvent(event)))
        } else Pair(emptyList(), emptySet())
      }

  private fun determineNews(event: SpecificRecordBase): List<News> =
      if (event is TaskEventAvro && event.name != TaskEventEnumAvro.DELETED) {
        determineNewsFromTaskEvent(event)
      } else if (event is TaskScheduleEventAvro) {
        determineNewsFromTaskScheduleEvent(event)
      } else if (event is TaskAttachmentEventAvro &&
          event.name != TaskAttachmentEventEnumAvro.DELETED) {
        determineNewsFromTaskAttachmentEvent(event)
      } else if (event is TopicEventG2Avro && event.name != TopicEventEnumAvro.DELETED) {
        determineNewsFromTopicEvent(event)
      } else if (event is TopicAttachmentEventAvro) {
        determineNewsFromTopicAttachmentEvent(event)
      } else if (event is MessageEventAvro && event.name != MessageEventEnumAvro.DELETED) {
        determineNewsFromMessageEvent(event)
      } else if (event is MessageAttachmentEventAvro) {
        determineNewsFromMessageAttachmentEvent(event)
      } else {
        listOf()
      }

  private fun determineNewsFromTaskScheduleEvent(event: TaskScheduleEventAvro): List<News> {
    val aggregate = event.aggregate
    val taskIdentifier = ObjectIdentifier(aggregate.task)
    val taskScheduleIdentifier = ObjectIdentifier(aggregate.aggregateIdentifier)

    return listOf(
        mapToNews(
            taskIdentifier,
            taskIdentifier,
            taskIdentifier,
            aggregate.auditingInformation.lastModifiedDate.toInstantByMillis()),
        mapToNews(
            taskScheduleIdentifier,
            taskIdentifier,
            taskIdentifier,
            aggregate.auditingInformation.lastModifiedDate.toInstantByMillis()))
  }

  private fun determineNewsFromTaskAttachmentEvent(event: TaskAttachmentEventAvro): List<News> {
    val aggregate = event.aggregate
    val taskIdentifier = ObjectIdentifier(aggregate.task)
    val taskAttachmentIdentifier = ObjectIdentifier(aggregate.aggregateIdentifier)

    return listOf(
        mapToNews(
            taskIdentifier,
            taskIdentifier,
            taskIdentifier,
            aggregate.auditingInformation.lastModifiedDate.toInstantByMillis()),
        mapToNews(
            taskAttachmentIdentifier,
            taskIdentifier,
            taskIdentifier,
            aggregate.auditingInformation.lastModifiedDate.toInstantByMillis()))
  }

  private fun determineNewsFromTaskEvent(event: TaskEventAvro): List<News> {
    val aggregate = event.aggregate
    val taskIdentifier = ObjectIdentifier(aggregate.aggregateIdentifier)

    return listOf(
        mapToNews(
            taskIdentifier,
            taskIdentifier,
            taskIdentifier,
            aggregate.auditingInformation.lastModifiedDate.toInstantByMillis()))
  }

  private fun determineNewsFromTopicEvent(event: TopicEventG2Avro): List<News> {
    val aggregate = event.aggregate
    val taskIdentifier = ObjectIdentifier(aggregate.task)
    val topicIdentifier = ObjectIdentifier(aggregate.aggregateIdentifier)

    return determineNewsFromTopicEvent(
        taskIdentifier, topicIdentifier, aggregate.auditingInformation.lastModifiedDate)
  }

  private fun determineNewsFromTopicEvent(
      taskIdentifier: ObjectIdentifier,
      topicIdentifier: ObjectIdentifier,
      lastModifiedDate: Long
  ): List<News> =
      listOf(
          mapToNews(
              taskIdentifier, taskIdentifier, taskIdentifier, lastModifiedDate.toInstantByMillis()),
          mapToNews(
              topicIdentifier,
              taskIdentifier,
              taskIdentifier,
              lastModifiedDate.toInstantByMillis()))

  private fun determineNewsFromTopicAttachmentEvent(event: TopicAttachmentEventAvro): List<News> {
    val aggregate = event.aggregate
    val topicIdentifier = ObjectIdentifier(aggregate.topic)
    val topicAttachmentIdentifier = ObjectIdentifier(aggregate.aggregateIdentifier)
    val taskIdentifier = objectRelationService.findTaskByTopic(topicIdentifier)

    checkNotNull(taskIdentifier) {
      "Could not find topic to task relation for topic with id: $topicIdentifier"
    }

    return listOf(
        mapToNews(
            taskIdentifier,
            taskIdentifier,
            taskIdentifier,
            aggregate.auditingInformation.lastModifiedDate.toInstantByMillis()),
        mapToNews(
            topicIdentifier,
            taskIdentifier,
            taskIdentifier,
            aggregate.auditingInformation.lastModifiedDate.toInstantByMillis()),
        mapToNews(
            topicAttachmentIdentifier,
            topicIdentifier,
            taskIdentifier,
            aggregate.auditingInformation.lastModifiedDate.toInstantByMillis()))
  }

  private fun determineNewsFromMessageEvent(event: MessageEventAvro): List<News> {
    val aggregate = event.aggregate
    val messageIdentifier = ObjectIdentifier(aggregate.aggregateIdentifier)
    val topicIdentifier = ObjectIdentifier(aggregate.topic)
    val taskIdentifier = objectRelationService.findTaskByTopic(topicIdentifier)

    checkNotNull(taskIdentifier) {
      "Could not find topic to task relation for topic with id: $topicIdentifier"
    }

    return listOf(
        mapToNews(
            taskIdentifier,
            taskIdentifier,
            taskIdentifier,
            aggregate.auditingInformation.lastModifiedDate.toInstantByMillis()),
        mapToNews(
            topicIdentifier,
            taskIdentifier,
            taskIdentifier,
            aggregate.auditingInformation.lastModifiedDate.toInstantByMillis()),
        mapToNews(
            messageIdentifier,
            topicIdentifier,
            taskIdentifier,
            aggregate.auditingInformation.lastModifiedDate.toInstantByMillis()))
  }

  private fun determineNewsFromMessageAttachmentEvent(
      event: MessageAttachmentEventAvro
  ): List<News> {
    val aggregate = event.aggregate
    val messageIdentifier = ObjectIdentifier(aggregate.message)
    val messageAttachmentIdentifier = ObjectIdentifier(aggregate.aggregateIdentifier)

    val topicIdentifier = objectRelationService.findTopicByMessage(messageIdentifier)
    checkNotNull(topicIdentifier) {
      "Could not find message to topic relation for message with id: $messageIdentifier"
    }

    val taskIdentifier = objectRelationService.findTaskByTopic(topicIdentifier)
    checkNotNull(taskIdentifier) {
      "Could not find topic to task relation for topic with id: $topicIdentifier"
    }

    return listOf(
        mapToNews(
            taskIdentifier,
            taskIdentifier,
            taskIdentifier,
            aggregate.auditingInformation.lastModifiedDate.toInstantByMillis()),
        mapToNews(
            topicIdentifier,
            taskIdentifier,
            taskIdentifier,
            aggregate.auditingInformation.lastModifiedDate.toInstantByMillis()),
        mapToNews(
            messageIdentifier,
            topicIdentifier,
            taskIdentifier,
            aggregate.auditingInformation.lastModifiedDate.toInstantByMillis()),
        mapToNews(
            messageAttachmentIdentifier,
            messageIdentifier,
            taskIdentifier,
            aggregate.auditingInformation.lastModifiedDate.toInstantByMillis()))
  }

  private fun determineRecipients(
      taskIdentifier: ObjectIdentifier,
      currenUserIdentifier: UUID
  ): Set<UUID> {
    val recipients: MutableSet<UUID> = HashSet()

    // determine the construction site manager of the project as a recipient
    val project = objectRelationService.findProjectByTask(taskIdentifier)
    checkNotNull(project) {
      "Could not find task to project relation for task with id: $taskIdentifier"
    }

    val csms =
        participantMappingRepository.findAllByProjectIdentifierAndParticipantRole(
            project.identifier, CSM.name)

    recipients.addAll(csms.map { it.userIdentifier }.toSet())

    // determine the assigned foreman and the corresponding company representatives as additional
    // recipients
    val assignedUser = objectRelationService.findAssignedUserForTask(taskIdentifier)

    // foreman any company representatives can only be found in case the task is already assigned
    if (assignedUser != null) {
      val assignedParticipant =
          participantMappingRepository.findOneByProjectIdentifierAndUserIdentifier(
              project.identifier, assignedUser.identifier)

      if (assignedParticipant != null && assignedParticipant.isNotCsm()) {
        recipients.add(assignedUser.identifier)

        val foremanCompany = objectRelationService.findAssignedCompanyForTask(taskIdentifier)
        checkNotNull(foremanCompany) {
          "Could not find task to company relation for task with id: $taskIdentifier"
        }

        val crs =
            participantMappingRepository
                .findAllByProjectIdentifierAndParticipantRoleAndCompanyIdentifier(
                    project.identifier, CR.name, foremanCompany.identifier)

        // in case the task is assigned to the CSM, no participant with role CR exists
        // can also happen if the participant that is the CR was deactivated
        recipients.addAll(crs.map { it.userIdentifier }.toSet())
      }
    }

    // remove the user that initiated the change from the list of recipients
    return recipients.filter { it != currenUserIdentifier }.toSet()
  }

  private fun mapToNews(
      contextObject: ObjectIdentifier,
      parentObject: ObjectIdentifier,
      rootObject: ObjectIdentifier,
      date: Instant
  ): News = News(rootObject, parentObject, contextObject, null, date, date)

  private fun getLastModifiedByUserFromEvent(source: SpecificRecordBase): UUID {
    var currentNode: Any = source
    for (property in arrayOf("aggregate", "auditingInformation", "lastModifiedBy", "identifier")) {
      currentNode = (currentNode as SpecificRecordBase)[property]
    }
    return (currentNode as String).toUUID()
  }

  private fun ParticipantMapping.isNotCsm(): Boolean = this.participantRole != CSM.name
}
