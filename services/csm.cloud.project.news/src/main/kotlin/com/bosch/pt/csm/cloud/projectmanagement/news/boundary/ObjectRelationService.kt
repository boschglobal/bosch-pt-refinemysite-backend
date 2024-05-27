/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.news.boundary

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.COMPANY
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.EMPLOYEE
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.MESSAGE
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.MESSAGE_ATTACHMENT
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.TASK
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.TASK_ATTACHMENT
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.TASK_SCHEDULE
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.TOPIC_ATTACHMENT
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.USER
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ObjectRelationRepository
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectRelation
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventG2Avro
import com.google.common.collect.Lists
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ObjectRelationService(
    @Value("\${db.in.max-size}") private val partitionSize: Int,
    private val objectRelationRepository: ObjectRelationRepository
) {

  @Trace
  fun saveTaskToAssignedUserRelation(event: TaskEventAvro) {
    val toEmployeeRelation =
        objectRelationRepository.findOneByLeftAndRightType(
            ObjectIdentifier(event.aggregate.assignee), EMPLOYEE)

    val user: ObjectRelation =
        if (toEmployeeRelation == null) {
          val user =
              objectRelationRepository.findOneByLeftAndRightType(
                  ObjectIdentifier(event.aggregate.assignee), USER)
          checkNotNull(user) {
            "Employee ID or User ID not found for assigned participant ${event.aggregate.assignee.identifier}"
          }
        } else {
          val employee = toEmployeeRelation.right
          val user =
              objectRelationRepository.findOneByLeftAndRightType(checkNotNull(employee), USER)
          checkNotNull(user) { "User ID not found for employee with ID ${employee.identifier}" }
        }

    val toAssignedUserRelation: ObjectRelation =
        objectRelationRepository
            .findOneByLeftAndRightType(ObjectIdentifier(event.aggregate.aggregateIdentifier), USER)
            ?.apply { this.right = user.right }
            ?: fromAggregate(event.aggregate.aggregateIdentifier, user.right)

    objectRelationRepository.save(toAssignedUserRelation)
  }

  @Trace
  fun deleteTaskToAssignedUserRelation(event: TaskEventAvro) =
      objectRelationRepository
          .findOneByLeftAndRightType(ObjectIdentifier(event.aggregate.aggregateIdentifier), USER)
          ?.let { objectRelationRepository.delete(it) }

  @Trace
  fun saveTaskToAssignedCompanyRelation(event: TaskEventAvro) {
    val toEmployeeRelation =
        objectRelationRepository.findOneByLeftAndRightType(
            ObjectIdentifier(event.aggregate.assignee), EMPLOYEE)

    val company =
        if (toEmployeeRelation == null) {
          checkNotNull(
              objectRelationRepository.findOneByLeftAndRightType(
                  ObjectIdentifier(event.aggregate.assignee), COMPANY)) {
                "Employee ID or Company ID not found for assigned participant ${event.aggregate.assignee.identifier}"
              }
        } else {
          val employee = toEmployeeRelation.right
          checkNotNull(
              objectRelationRepository.findOneByLeftAndRightType(checkNotNull(employee), COMPANY)) {
                "Company ID not found for employee with ID ${employee.identifier}"
              }
        }

    val toAssignedCompanyRelation =
        objectRelationRepository
            .findOneByLeftAndRightType(
                ObjectIdentifier(event.aggregate.aggregateIdentifier), COMPANY)
            ?.apply { this.right = company.right }
            ?: fromAggregate(event.aggregate.aggregateIdentifier, company.right)

    objectRelationRepository.save(toAssignedCompanyRelation)
  }

  @Trace
  fun deleteTaskToAssignedCompanyRelation(event: TaskEventAvro) =
      objectRelationRepository
          .findOneByLeftAndRightType(ObjectIdentifier(event.aggregate.aggregateIdentifier), COMPANY)
          ?.let { objectRelationRepository.delete(it) }

  @Trace
  fun saveTaskToProjectRelation(event: TaskEventAvro) {
    val toProjectRelation =
        objectRelationRepository
            .findOneByLeftAndRightType(
                ObjectIdentifier(event.aggregate.aggregateIdentifier), PROJECT)
            ?.apply { this.right = ObjectIdentifier(event.aggregate.project) }
            ?: fromAggregate(event.aggregate.aggregateIdentifier, event.aggregate.project)

    objectRelationRepository.save(toProjectRelation)
  }

  @Trace
  fun saveTaskScheduleToTaskRelation(event: TaskScheduleEventAvro) =
      saveOrUpdate(event.aggregate.aggregateIdentifier, event.aggregate.task)

  @Trace
  fun saveTaskAttachmentToTaskRelation(event: TaskAttachmentEventAvro) =
      saveOrUpdate(event.aggregate.aggregateIdentifier, event.aggregate.task)

  @Trace
  fun saveTopicToTaskRelation(event: TopicEventG2Avro) =
      saveOrUpdate(event.aggregate.aggregateIdentifier, event.aggregate.task)

  @Trace
  fun saveTopicAttachmentToTopicRelation(event: TopicAttachmentEventAvro) =
      saveOrUpdate(event.aggregate.aggregateIdentifier, event.aggregate.topic)

  @Trace
  fun saveMessageAttachmentToMessageRelation(event: MessageAttachmentEventAvro) =
      saveOrUpdate(event.aggregate.aggregateIdentifier, event.aggregate.message)

  @Trace
  fun saveEmployeeToCompanyRelation(event: EmployeeEventAvro) =
      saveOrUpdate(event.aggregate.aggregateIdentifier, event.aggregate.company)

  @Trace
  fun saveEmployeeToUserRelation(event: EmployeeEventAvro) =
      saveOrUpdate(event.aggregate.aggregateIdentifier, event.aggregate.user)

  @Trace
  fun saveParticipantToUserRelation(event: ParticipantEventG3Avro) =
      saveOrUpdate(event.aggregate.aggregateIdentifier, event.aggregate.user)

  @Trace
  fun saveParticipantToCompanyRelation(event: ParticipantEventG3Avro) =
      saveOrUpdate(event.aggregate.aggregateIdentifier, event.aggregate.company)

  @Trace
  fun saveParticipantToProjectRelation(event: ParticipantEventG3Avro) =
      saveOrUpdate(event.aggregate.aggregateIdentifier, event.aggregate.project)

  @Trace
  fun saveMessageToTopicRelation(event: MessageEventAvro) =
      saveOrUpdate(event.aggregate.aggregateIdentifier, event.aggregate.topic)

  @Trace
  fun findProjectByTask(taskIdentifier: ObjectIdentifier): ObjectIdentifier? =
      objectRelationRepository.findOneByLeftAndRightType(taskIdentifier, PROJECT)?.right

  @Trace
  fun findTaskByTopic(topicIdentifier: ObjectIdentifier): ObjectIdentifier? =
      objectRelationRepository.findOneByLeftAndRightType(topicIdentifier, TASK)?.right

  @Trace
  fun findTopicByMessage(messageIdentifier: ObjectIdentifier): ObjectIdentifier? =
      objectRelationRepository
          .findOneByLeftAndRightType(messageIdentifier, AggregateType.TOPIC)
          ?.right

  // can be null in case the task is not assigned yet
  @Trace
  fun findAssignedUserForTask(taskIdentifier: ObjectIdentifier): ObjectIdentifier? =
      objectRelationRepository.findOneByLeftAndRightType(taskIdentifier, USER)?.right

  @Trace
  fun findAssignedCompanyForTask(taskIdentifier: ObjectIdentifier): ObjectIdentifier? =
      objectRelationRepository.findOneByLeftAndRightType(taskIdentifier, COMPANY)?.right

  @Trace
  fun findTaskIdentifiers(projectIdentifier: AggregateIdentifierAvro): List<ObjectIdentifier> =
      getLeftObjectIdentifiers(
          objectRelationRepository.findAllByLeftTypeAndRight(
              TASK, ObjectIdentifier(projectIdentifier)))

  @Trace
  fun findTaskIdentifiers(projectIdentifier: UUID): List<ObjectIdentifier> =
      getLeftObjectIdentifiers(
          objectRelationRepository.findAllByLeftTypeAndRight(
              TASK, ObjectIdentifier(PROJECT, projectIdentifier)))

  @Trace
  fun deleteByProjectIdentifier(projectIdentifier: AggregateIdentifierAvro) {
    val projectObjectIdentifier = ObjectIdentifier(projectIdentifier)

    val idsToDelete =
        mutableListOf<Long>().apply {

          // Task - Project
          val taskRelations =
              objectRelationRepository.findAllByLeftTypeAndRight(TASK, projectObjectIdentifier)
          addAll(getIdsOfRelations(taskRelations))

          // Topic - Task
          val taskIdentifiers = getLeftObjectIdentifiers(taskRelations)
          val topicIdentifiers =
              findRelationsOfSpecificTypePointingToIdentifiers(AggregateType.TOPIC, taskIdentifiers)
          addAll(getIdsOfRelations(topicIdentifiers))

          // Topic Attachment - Topic
          addAll(
              getIdsOfRelations(
                  findRelationsOfSpecificTypePointingToIdentifiers(
                      TOPIC_ATTACHMENT, getLeftObjectIdentifiers(topicIdentifiers))))

          // Message - Topic
          val messageIdentifiers =
              findRelationsOfSpecificTypePointingToIdentifiers(
                  MESSAGE, getLeftObjectIdentifiers(topicIdentifiers))
          addAll(getIdsOfRelations(messageIdentifiers))

          // Message Attachment - Message
          addAll(
              getIdsOfRelations(
                  findRelationsOfSpecificTypePointingToIdentifiers(
                      MESSAGE_ATTACHMENT, getLeftObjectIdentifiers(messageIdentifiers))))

          // Task Attachment - Task
          addAll(
              getIdsOfRelations(
                  findRelationsOfSpecificTypePointingToIdentifiers(
                      TASK_ATTACHMENT, taskIdentifiers)))

          // TaskSchedule - Task
          addAll(
              getIdsOfRelations(
                  findRelationsOfSpecificTypePointingToIdentifiers(TASK_SCHEDULE, taskIdentifiers)))

          // Task - User
          addAll(
              getIdsOfRelations(
                  objectRelationRepository.findAllByLeftInAndRightType(taskIdentifiers, USER)))

          // Task - Company
          addAll(
              getIdsOfRelations(
                  objectRelationRepository.findAllByLeftInAndRightType(taskIdentifiers, COMPANY)))

          // Participant - Project
          val participants =
              objectRelationRepository.findAllByLeftTypeAndRight(
                  AggregateType.PARTICIPANT, projectObjectIdentifier)
          addAll(getIdsOfRelations(participants))

          // Participant - Employee
          addAll(
              getIdsOfRelations(
                  objectRelationRepository.findAllByLeftInAndRightType(
                      getLeftObjectIdentifiers(participants), EMPLOYEE)))
        }

    objectRelationRepository.deletePartitioned(idsToDelete)
  }

  @Trace
  fun deleteByTaskIdentifier(taskIdentifier: AggregateIdentifierAvro) {
    val taskObjectIdentifier = ObjectIdentifier(taskIdentifier)

    val idsToDelete =
        mutableListOf<Long>().apply {

          // Topic - Task
          val topicIdentifiers =
              findRelationsOfSpecificTypePointingToIdentifiers(
                  AggregateType.TOPIC, listOf(taskObjectIdentifier))
          addAll(getIdsOfRelations(topicIdentifiers))

          // Topic Attachment - Topic
          val topicAttachmentIdentifiers =
              findRelationsOfSpecificTypePointingToIdentifiers(
                  TOPIC_ATTACHMENT, getLeftObjectIdentifiers(topicIdentifiers))
          addAll(getIdsOfRelations(topicAttachmentIdentifiers))

          // Message - Topic
          val messageIdentifiers =
              findRelationsOfSpecificTypePointingToIdentifiers(
                  MESSAGE, getLeftObjectIdentifiers(topicIdentifiers))
          addAll(getIdsOfRelations(messageIdentifiers))

          // Message Attachment - Message
          val messageAttachmentIdentifiers =
              findRelationsOfSpecificTypePointingToIdentifiers(
                  MESSAGE_ATTACHMENT, getLeftObjectIdentifiers(messageIdentifiers))
          addAll(getIdsOfRelations(messageAttachmentIdentifiers))
        }

    // Check simple * to task relations
    addAnyToIdentifierRelationForDeletion(idsToDelete, TASK_ATTACHMENT, taskObjectIdentifier)
    addAnyToIdentifierRelationForDeletion(idsToDelete, TASK_SCHEDULE, taskObjectIdentifier)

    // Check task to * relations
    addIdentifierToAnyRelationForDeletion(idsToDelete, taskObjectIdentifier, USER)
    addIdentifierToAnyRelationForDeletion(idsToDelete, taskObjectIdentifier, COMPANY)
    addIdentifierToAnyRelationForDeletion(idsToDelete, taskObjectIdentifier, PROJECT)

    objectRelationRepository.deletePartitioned(idsToDelete)
  }

  @Trace
  fun deleteByTaskAttachmentIdentifier(taskAttachmentIdentifier: AggregateIdentifierAvro) {
    val taskAttachmentToTaskRelation =
        checkNotNull(
            objectRelationRepository.findOneByLeftAndRightType(
                ObjectIdentifier(taskAttachmentIdentifier), TASK))

    objectRelationRepository.delete(taskAttachmentToTaskRelation)
  }

  @Trace
  fun deleteByTopicIdentifier(topicIdentifier: AggregateIdentifierAvro) {
    val topicObjectIdentifier = ObjectIdentifier(topicIdentifier)

    val idsToDelete =
        mutableListOf<Long>().apply {

          // Message - Topic
          val messageIdentifiers =
              findRelationsOfSpecificTypePointingToIdentifiers(
                  MESSAGE, listOf(topicObjectIdentifier))
          addAll(getIdsOfRelations(messageIdentifiers))

          // Message Attachment - Message
          val messageAttachmentIdentifiers =
              findRelationsOfSpecificTypePointingToIdentifiers(
                  MESSAGE_ATTACHMENT, getLeftObjectIdentifiers(messageIdentifiers))
          addAll(getIdsOfRelations(messageAttachmentIdentifiers))
        }

    addAnyToIdentifierRelationForDeletion(idsToDelete, TOPIC_ATTACHMENT, topicObjectIdentifier)
    addIdentifierToAnyRelationForDeletion(idsToDelete, topicObjectIdentifier, TASK)

    objectRelationRepository.deletePartitioned(idsToDelete)
  }

  @Trace
  fun deleteByMessageIdentifier(messageIdentifier: AggregateIdentifierAvro) {
    val messageObjectIdentifier = ObjectIdentifier(messageIdentifier)

    val idsToDelete =
        mutableListOf<Long>().apply {
          addIdentifierToAnyRelationForDeletion(this, messageObjectIdentifier, AggregateType.TOPIC)
          addAnyToIdentifierRelationForDeletion(this, MESSAGE_ATTACHMENT, messageObjectIdentifier)
        }

    objectRelationRepository.deletePartitioned(idsToDelete)
  }

  private fun getLeftObjectIdentifiers(relations: List<ObjectRelation>): List<ObjectIdentifier> =
      relations.map { it.left }

  private fun getIdsOfRelations(relations: List<ObjectRelation>): List<Long> =
      relations.map { checkNotNull(it.id) }

  private fun findRelationsOfSpecificTypePointingToIdentifiers(
      type: String,
      identifiers: List<ObjectIdentifier>
  ): List<ObjectRelation> =
      Lists.partition(identifiers, partitionSize)
          .map { partition: List<ObjectIdentifier> ->
            objectRelationRepository.findAllByLeftTypeAndRightIn(type, partition)
          }
          .flatten()

  private fun addAnyToIdentifierRelationForDeletion(
      idsToDelete: MutableList<Long>,
      type: String,
      identifier: ObjectIdentifier
  ) {
    idsToDelete.addAll(
        objectRelationRepository.findAllByLeftTypeAndRight(type, identifier).map {
          checkNotNull(it.id)
        })
  }

  private fun addIdentifierToAnyRelationForDeletion(
      idsToDelete: MutableList<Long>,
      identifier: ObjectIdentifier,
      type: String
  ) =
      objectRelationRepository.findOneByLeftAndRightType(identifier, type)?.let {
        idsToDelete.add(checkNotNull(it.id))
      }

  private fun saveOrUpdate(left: AggregateIdentifierAvro, right: AggregateIdentifierAvro) {
    val newRelation = fromAggregate(left, right)
    val relation =
        objectRelationRepository.findOneByLeftAndRightType(newRelation.left, newRelation.right.type)

    if (relation == null) {
      objectRelationRepository.save(newRelation)
    } else {
      relation.right = newRelation.right
    }
  }

  private fun fromAggregate(
      left: AggregateIdentifierAvro,
      right: AggregateIdentifierAvro
  ): ObjectRelation = fromAggregate(left, ObjectIdentifier(right))

  private fun fromAggregate(left: AggregateIdentifierAvro, right: ObjectIdentifier) =
      ObjectRelation(left = ObjectIdentifier(left), right = right)
}
