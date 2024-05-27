/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Activity
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Summary
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.activity.AbstractActivityStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UNASSIGNED
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.service.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.message.buildContext
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.message.buildEventInformation
import com.bosch.pt.csm.cloud.projectmanagement.project.task.model.Task
import com.bosch.pt.csm.cloud.projectmanagement.project.task.service.TaskService
import com.bosch.pt.csm.cloud.projectmanagement.task.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.UNASSIGNED
import datadog.trace.api.Trace
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component
import org.springframework.util.IdGenerator

@Component
class TaskUnassignedActivityStrategy(
    private val participantService: ParticipantService,
    private val taskService: TaskService,
    private val idGenerator: IdGenerator
) : AbstractActivityStrategy<TaskEventAvro>() {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is TaskEventAvro && value.getName() == UNASSIGNED

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

  private fun buildSummary(projectIdentifier: UUID, taskEventAvro: TaskEventAvro): Summary =
      Summary(
          templateMessageKey = TASK_ACTIVITY_UNASSIGNED,
          references =
              mapOf(
                  "originator" to buildOriginatorReference(taskEventAvro, projectIdentifier),
                  "assignee" to buildAssigneeReference(taskEventAvro, projectIdentifier)))

  private fun buildAssigneeReference(
      taskEventAvro: TaskEventAvro,
      projectIdentifier: UUID
  ): UnresolvedObjectReference {
    val previousTask = findPreviousTaskVersion(taskEventAvro.getAggregate(), projectIdentifier)
    return UnresolvedObjectReference(
        type = PARTICIPANT.type,
        identifier = previousTask.assigneeIdentifier!!,
        contextRootIdentifier = projectIdentifier)
  }

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

  private fun findPreviousTaskVersion(
      taskAggregateAvro: TaskAggregateAvro,
      projectIdentifier: UUID
  ): Task {
    val taskIdentifier = taskAggregateAvro.getAggregateIdentifier().getIdentifier().toUUID()
    val taskVersion = taskAggregateAvro.getAggregateIdentifier().getVersion()
    return taskService.find(taskIdentifier, taskVersion - 1, projectIdentifier)
  }
}
