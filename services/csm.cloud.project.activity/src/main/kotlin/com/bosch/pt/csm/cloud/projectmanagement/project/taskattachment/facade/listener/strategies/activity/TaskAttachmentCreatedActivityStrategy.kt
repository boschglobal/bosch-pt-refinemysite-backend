/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Activity
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.AttributeChanges
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ChangeDescription
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Context
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.SimpleString
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Summary
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.activity.AbstractActivityStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.BARE_PARAMETERS_ONE_PARAMETER
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ATTACHMENT_ACTIVITY_SAVED
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.buildAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.buildAuditingInformation
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.service.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.facade.listener.message.buildEventInformation
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.taskattachment.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskattachment.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskattachment.message.getTaskIdentifier
import datadog.trace.api.Trace
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component
import org.springframework.util.IdGenerator

@Component
class TaskAttachmentCreatedActivityStrategy(
    private val idGenerator: IdGenerator,
    private val participantService: ParticipantService
) : AbstractActivityStrategy<TaskAttachmentEventAvro>() {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is TaskAttachmentEventAvro && value.getName() == CREATED

  @Trace
  override fun createActivity(key: EventMessageKey, event: TaskAttachmentEventAvro): Activity {
    val projectIdentifier = key.rootContextIdentifier

    return event.getAggregate().run {
      Activity(
          aggregateIdentifier = getAggregateIdentifier().buildAggregateIdentifier(),
          identifier = idGenerator.generateId(),
          event = event.buildEventInformation(),
          summary = buildSummary(projectIdentifier, event),
          details = buildDetails(this),
          context = Context(project = projectIdentifier, task = event.getTaskIdentifier()),
          attachment = buildAttachment(projectIdentifier, this))
    }
  }

  private fun buildSummary(projectIdentifier: UUID, event: TaskAttachmentEventAvro): Summary {
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
        templateMessageKey = TASK_ATTACHMENT_ACTIVITY_SAVED,
        references = mapOf("originator" to originator))
  }

  private fun buildDetails(aggregate: TaskAttachmentAggregateAvro) =
      AttributeChanges(
          listOf(
              ChangeDescription(
                  BARE_PARAMETERS_ONE_PARAMETER,
                  listOf(SimpleString(aggregate.getAttachment().getFileName())))))

  private fun buildAttachment(projectIdentifier: UUID, aggregate: TaskAttachmentAggregateAvro) =
      aggregate
          .getAttachment()
          .buildAttachment(
              auditingInformation =
                  aggregate.getAuditingInformation().buildAuditingInformation(projectIdentifier),
              identifier = aggregate.getIdentifier(),
              taskId = aggregate.getTaskIdentifier())
}
