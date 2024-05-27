/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Activity
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.AttributeChanges
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ChangeDescription
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Details
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.SimpleDate
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Summary
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.activity.AbstractActivityStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_DELETED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_DELETED_END
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_DELETED_START
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.service.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.message.buildContext
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.message.buildEventInformation
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.service.TaskScheduleService
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getVersion
import datadog.trace.api.Trace
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component
import org.springframework.util.IdGenerator

@Component
class TaskScheduleDeletedActivityStrategy(
    private val participantService: ParticipantService,
    private val taskScheduleService: TaskScheduleService,
    private val idGenerator: IdGenerator
) : AbstractActivityStrategy<TaskScheduleEventAvro>() {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is TaskScheduleEventAvro && value.getName() == DELETED

  @Trace
  override fun createActivity(key: EventMessageKey, event: TaskScheduleEventAvro): Activity {
    val projectIdentifier = key.rootContextIdentifier

    return event.getAggregate().run {
      Activity(
          aggregateIdentifier = buildAggregateIdentifier(),
          identifier = idGenerator.generateId(),
          event = event.buildEventInformation(),
          summary = buildSummary(projectIdentifier, event),
          details = buildDetails(projectIdentifier, this),
          context = buildContext(projectIdentifier))
    }
  }

  private fun buildSummary(projectIdentifier: UUID, event: TaskScheduleEventAvro): Summary {
    val userIdentifier = event.getLastModifiedByUserIdentifier()

    val originatorParticipant =
        participantService.findOneByProjectIdentifierAndUserIdentifier(
            projectIdentifier, userIdentifier)

    val originator =
        UnresolvedObjectReference(
            type = PARTICIPANT.type,
            identifier = originatorParticipant.identifier,
            contextRootIdentifier = projectIdentifier)

    return Summary(
        templateMessageKey = TASK_SCHEDULE_ACTIVITY_DELETED,
        references = mapOf("originator" to originator))
  }

  private fun buildDetails(projectIdentifier: UUID, aggregate: TaskScheduleAggregateAvro): Details {
    val identifier = aggregate.getIdentifier()
    val version = aggregate.getVersion()
    val previousVersion = taskScheduleService.find(identifier, version - 1, projectIdentifier)

    val start =
        if (previousVersion.start != null)
            ChangeDescription(
                templateMessageKey = TASK_SCHEDULE_ACTIVITY_DELETED_START,
                values = listOf(SimpleDate(previousVersion.start)))
        else null

    val end =
        if (previousVersion.end != null)
            ChangeDescription(
                templateMessageKey = TASK_SCHEDULE_ACTIVITY_DELETED_END,
                values = listOf(SimpleDate(previousVersion.end)))
        else null

    return AttributeChanges(attributes = listOfNotNull(start, end))
  }
}
