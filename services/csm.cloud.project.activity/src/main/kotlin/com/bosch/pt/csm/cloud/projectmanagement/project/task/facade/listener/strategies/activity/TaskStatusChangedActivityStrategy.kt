/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Activity
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Summary
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.activity.AbstractActivityStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_ACCEPTED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_FINISHED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_SENT
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_SENT_WITHOUT_ASSIGNEE
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_STARTED
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.service.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.message.buildContext
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.message.buildEventInformation
import com.bosch.pt.csm.cloud.projectmanagement.task.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getAssigneeIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.ACCEPTED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.CLOSED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.SENT
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.STARTED
import datadog.trace.api.Trace
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component
import org.springframework.util.IdGenerator

@Component
class TaskStatusChangedActivityStrategy(
    private val participantService: ParticipantService,
    private val idGenerator: IdGenerator
) : AbstractActivityStrategy<TaskEventAvro>() {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is TaskEventAvro && value.getName() in arrayOf(CLOSED, SENT, STARTED, ACCEPTED)

  @Trace
  override fun createActivity(key: EventMessageKey, event: TaskEventAvro): Activity {
    val projectIdentifier = key.rootContextIdentifier

    return event.getAggregate().run {
      Activity(
          aggregateIdentifier = buildAggregateIdentifier(),
          identifier = idGenerator.generateId(),
          event = event.buildEventInformation(),
          summary = buildSummary(projectIdentifier, event),
          context = buildContext(projectIdentifier))
    }
  }

  private fun buildSummary(projectIdentifier: UUID, taskEventAvro: TaskEventAvro): Summary {
    val originatorParticipant = buildOriginatorReference(taskEventAvro, projectIdentifier)

    return when (taskEventAvro.getName()) {
      STARTED ->
          Summary(
              templateMessageKey = TASK_ACTIVITY_STARTED,
              references = mapOf("originator" to originatorParticipant))
      SENT ->
          if (taskEventAvro.getAssigneeIdentifier() != null) {
            Summary(
                templateMessageKey = TASK_ACTIVITY_SENT,
                references =
                    mapOf(
                        "originator" to originatorParticipant,
                        "assignee" to buildAssigneeReference(taskEventAvro, projectIdentifier)))
          } else {
            Summary(
                templateMessageKey = TASK_ACTIVITY_SENT_WITHOUT_ASSIGNEE,
                references = mapOf("originator" to originatorParticipant))
          }
      CLOSED ->
          Summary(
              templateMessageKey = TASK_ACTIVITY_FINISHED,
              references = mapOf("originator" to originatorParticipant))
      ACCEPTED ->
          Summary(
              templateMessageKey = TASK_ACTIVITY_ACCEPTED,
              references = mapOf("originator" to originatorParticipant))
      else -> throw IllegalStateException("Unable to handle event type ${taskEventAvro.getName()}")
    }
  }

  private fun buildAssigneeReference(taskEventAvro: TaskEventAvro, projectIdentifier: UUID) =
      UnresolvedObjectReference(
          type = PARTICIPANT.type,
          identifier = taskEventAvro.getAssigneeIdentifier()!!,
          contextRootIdentifier = projectIdentifier)

  private fun buildOriginatorReference(
      taskEventAvro: TaskEventAvro,
      projectIdentifier: UUID
  ): ObjectReference {
    val originatorParticipant =
        participantService.findOneByProjectIdentifierAndUserIdentifier(
            projectIdentifier, taskEventAvro.getLastModifiedByUserIdentifier())

    return UnresolvedObjectReference(
        type = PARTICIPANT.type,
        identifier = originatorParticipant.identifier,
        contextRootIdentifier = projectIdentifier)
  }
}
