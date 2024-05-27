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
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.AttributeChanges
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ChangeDescription
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Details
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ResolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.SimpleString
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Summary
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.TASK
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.activity.AbstractActivityStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_ASSIGNEE
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_CRAFT
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_DESCRIPTION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_LOCATION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_NAME
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_STATUS
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_WORKAREA
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.service.ProjectCraftService
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.service.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.message.buildContext
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.message.buildEventInformation
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.service.WorkAreaService
import com.bosch.pt.csm.cloud.projectmanagement.task.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getAssigneeIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getCraftIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getWorkAreaIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro.CREATED
import datadog.trace.api.Trace
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component
import org.springframework.util.IdGenerator

@Component
class TaskCreatedActivityStrategy(
    private val idGenerator: IdGenerator,
    private val participantService: ParticipantService,
    private val projectCraftService: ProjectCraftService,
    private val workAreaService: WorkAreaService
) : AbstractActivityStrategy<TaskEventAvro>() {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is TaskEventAvro && value.getName() == CREATED

  @Trace
  override fun createActivity(key: EventMessageKey, event: TaskEventAvro): Activity {
    val taskAggregateAvro = event.getAggregate()
    val projectIdentifier = key.rootContextIdentifier

    return event.getAggregate().run {
      Activity(
          aggregateIdentifier = buildAggregateIdentifier(),
          identifier = idGenerator.generateId(),
          event = event.buildEventInformation(),
          details = buildDetails(taskAggregateAvro, projectIdentifier),
          summary = buildSummary(projectIdentifier, event),
          context = buildContext(projectIdentifier))
    }
  }

  private fun buildDetails(taskAggregate: TaskAggregateAvro, projectIdentifier: UUID): Details {
    return AttributeChanges(
        createdTaskAttributes(taskAggregate, projectIdentifier).entries.sortedBy { it.key }.map {
          ChangeDescription(
              templateMessageKey = mapToMessageKey(it.key), values = listOf(it.value!!))
        })
  }

  private fun createdTaskAttributes(taskAggregateAvro: TaskAggregateAvro, projectIdentifier: UUID) =
      with(taskAggregateAvro) {
        mapOf(
                "assignee" to buildAssigneeAttributeValue(this, projectIdentifier),
                "craft" to buildCraftAttributeValue(this, projectIdentifier),
                "description" to getDescription()?.let { SimpleString(it) },
                "location" to getLocation()?.let { SimpleString(it) },
                "name" to SimpleString(getName()),
                "status" to SimpleString(getStatus().toString()),
                "workArea" to buildWorkAreaAttributeValue(this, projectIdentifier))
            .filterValues { it != null }
      }

  private fun buildCraftAttributeValue(
      taskAggregateAvro: TaskAggregateAvro,
      projectIdentifier: UUID
  ) =
      SimpleString(
          projectCraftService.findLatest(taskAggregateAvro.getCraftIdentifier(), projectIdentifier)
              .name)

  private fun buildWorkAreaAttributeValue(
      taskAggregateAvro: TaskAggregateAvro,
      projectIdentifier: UUID
  ) =
      taskAggregateAvro.getWorkAreaIdentifier()?.let {
        SimpleString(workAreaService.findLatest(it, projectIdentifier).name)
      }

  private fun buildAssigneeAttributeValue(
      taskAggregateAvro: TaskAggregateAvro,
      projectIdentifier: UUID
  ) =
      taskAggregateAvro.getAssigneeIdentifier()?.let {
        UnresolvedObjectReference(PARTICIPANT.type, it, projectIdentifier)
      }

  private fun mapToMessageKey(attribute: String) =
      when (attribute) {
        "assignee" -> TASK_ACTIVITY_CREATED_ASSIGNEE
        "craft" -> TASK_ACTIVITY_CREATED_CRAFT
        "description" -> TASK_ACTIVITY_CREATED_DESCRIPTION
        "location" -> TASK_ACTIVITY_CREATED_LOCATION
        "name" -> TASK_ACTIVITY_CREATED_NAME
        "status" -> TASK_ACTIVITY_CREATED_STATUS
        "workArea" -> TASK_ACTIVITY_CREATED_WORKAREA
        else -> throw IllegalStateException("Unknown task attribute: $attribute")
      }

  private fun buildSummary(projectIdentifier: UUID, taskEventAvro: TaskEventAvro): Summary {
    val userIdentifier = taskEventAvro.getLastModifiedByUserIdentifier()

    val originatorParticipant =
        participantService.findOneByProjectIdentifierAndUserIdentifier(
            projectIdentifier, userIdentifier)

    val originator =
        UnresolvedObjectReference(
            type = PARTICIPANT.type,
            identifier = originatorParticipant.identifier,
            contextRootIdentifier = projectIdentifier)

    val task =
        ResolvedObjectReference(
            type = TASK.type,
            identifier = taskEventAvro.getIdentifier(),
            displayName = taskEventAvro.getAggregate().getName())

    return Summary(
        templateMessageKey = TASK_ACTIVITY_CREATED,
        references = mapOf("originator" to originator, "task" to task))
  }
}
