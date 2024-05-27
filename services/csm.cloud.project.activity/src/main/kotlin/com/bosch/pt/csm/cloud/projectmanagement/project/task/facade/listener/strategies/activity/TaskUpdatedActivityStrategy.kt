/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Activity
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.AttributeChanges
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ChangeDescription
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Details
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.SimpleString
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Summary
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.activity.AbstractActivityStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_ASSIGNEE
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_CRAFT
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_DESCRIPTION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_LOCATION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_NAME
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_STATUS
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_CREATED_WORKAREA
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_REMOVED_WORKAREA
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED_ASSIGNEE
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED_ASSIGNEE_REMOVED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED_CRAFT
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED_DESCRIPTION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED_DESCRIPTION_REMOVED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED_LOCATION
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED_LOCATION_REMOVED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED_NAME
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED_STATUS
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_ACTIVITY_UPDATED_WORKAREA
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AggregateComparator
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AttributeChange
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AttributeChangeEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AttributeChangeEnum.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AttributeChangeEnum.REMOVED
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AttributeChangeEnum.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.service.ProjectCraftService
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.service.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.message.buildContext
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.listener.message.buildEventInformation
import com.bosch.pt.csm.cloud.projectmanagement.project.task.service.TaskService
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.service.WorkAreaService
import com.bosch.pt.csm.cloud.projectmanagement.task.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import datadog.trace.api.Trace
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component
import org.springframework.util.IdGenerator

@Component
class TaskUpdatedActivityStrategy(
    private val participantService: ParticipantService,
    private val projectCraftService: ProjectCraftService,
    private val taskService: TaskService,
    private val workAreaService: WorkAreaService,
    private val aggregateComparator: AggregateComparator,
    private val idGenerator: IdGenerator
) : AbstractActivityStrategy<TaskEventAvro>() {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is TaskEventAvro && value.getName() == TaskEventEnumAvro.UPDATED

  @Trace
  override fun createActivity(key: EventMessageKey, event: TaskEventAvro): Activity {
    val projectIdentifier = key.rootContextIdentifier

    return event.getAggregate().run {
      Activity(
          aggregateIdentifier = buildAggregateIdentifier(),
          identifier = idGenerator.generateId(),
          event = event.buildEventInformation(),
          details = buildDetails(projectIdentifier, event),
          summary = buildSummary(projectIdentifier, event),
          context = buildContext(projectIdentifier))
    }
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

    return Summary(
        templateMessageKey = TASK_ACTIVITY_UPDATED,
        references = mapOf("originator" to originator),
        values = mapOf("count" to countChanges(taskEventAvro)))
  }

  private fun countChanges(taskEventAvro: TaskEventAvro) =
      differenceToPrevious(taskEventAvro.getAggregate()).size.toString()

  private fun buildDetails(projectIdentifier: UUID, taskEventAvro: TaskEventAvro): Details {
    val changedAttributes = differenceToPrevious(taskEventAvro.getAggregate())
    return AttributeChanges(
        changedAttributes
            .sortedBy { it.attribute }
            .map {
              ChangeDescription(
                  mapToMessageKey(it.attribute, it.changeType),
                  mapToMessageValues(it, projectIdentifier))
            })
  }

  private fun mapToMessageValues(attribute: AttributeChange, projectIdentifier: UUID) =
      when (attribute.attribute) {
        "assigneeIdentifier" -> buildAssigneeAttributeValues(attribute, projectIdentifier)
        "description" -> buildSingleAttributeValues(attribute)
        "craftIdentifier" -> buildCraftAttributeValues(attribute, projectIdentifier)
        "status" -> buildSingleAttributeValues(attribute)
        "workAreaIdentifier" -> buildWorkAreaAttributeValues(attribute, projectIdentifier)
        else -> buildDefaultAttributeValues(attribute)
      }

  private fun buildAssigneeAttributeValues(attribute: AttributeChange, projectIdentifier: UUID) =
      listOfNotNull(attribute.oldValue, attribute.newValue)
          .map {
            participantService.findOneCacheByIdentifierAndProjectIdentifier(
                it as UUID, projectIdentifier)
          }
          .map { UnresolvedObjectReference(PARTICIPANT.type, it.identifier, projectIdentifier) }

  private fun buildCraftAttributeValues(attribute: AttributeChange, projectIdentifier: UUID) =
      listOfNotNull(attribute.oldValue, attribute.newValue).map {
        SimpleString(projectCraftService.findLatest(it as UUID, projectIdentifier).name)
      }

  private fun buildSingleAttributeValues(attribute: AttributeChange): List<SimpleString> {
    val value =
        when (attribute.changeType) {
          CREATED, UPDATED -> attribute.newValue
          REMOVED -> attribute.oldValue
        }
    return listOfNotNull(value).map { SimpleString(it as String) }
  }

  private fun buildWorkAreaAttributeValues(attribute: AttributeChange, projectIdentifier: UUID) =
      listOfNotNull(attribute.oldValue, attribute.newValue).map {
        SimpleString(workAreaService.findLatest(it as UUID, projectIdentifier).name)
      }

  private fun buildDefaultAttributeValues(attribute: AttributeChange) =
      listOfNotNull(attribute.oldValue, attribute.newValue).map { SimpleString(it as String) }

  private fun mapToMessageKey(attribute: String, event: AttributeChangeEnum) =
      when (event) {
        CREATED ->
            when (attribute) {
              "assigneeIdentifier" -> TASK_ACTIVITY_CREATED_ASSIGNEE
              "craftIdentifier" -> TASK_ACTIVITY_CREATED_CRAFT
              "description" -> TASK_ACTIVITY_CREATED_DESCRIPTION
              "location" -> TASK_ACTIVITY_CREATED_LOCATION
              "name" -> TASK_ACTIVITY_CREATED_NAME
              "status" -> TASK_ACTIVITY_CREATED_STATUS
              "workAreaIdentifier" -> TASK_ACTIVITY_CREATED_WORKAREA
              else -> throw IllegalStateException("Unknown task attribute: $attribute")
            }
        UPDATED ->
            when (attribute) {
              "assigneeIdentifier" -> TASK_ACTIVITY_UPDATED_ASSIGNEE
              "craftIdentifier" -> TASK_ACTIVITY_UPDATED_CRAFT
              "description" -> TASK_ACTIVITY_UPDATED_DESCRIPTION
              "location" -> TASK_ACTIVITY_UPDATED_LOCATION
              "name" -> TASK_ACTIVITY_UPDATED_NAME
              "status" -> TASK_ACTIVITY_UPDATED_STATUS
              "workAreaIdentifier" -> TASK_ACTIVITY_UPDATED_WORKAREA
              else -> throw IllegalStateException("Unknown task attribute: $attribute")
            }
        REMOVED ->
            when (attribute) {
              "assigneeIdentifier" -> TASK_ACTIVITY_UPDATED_ASSIGNEE_REMOVED
              "description" -> TASK_ACTIVITY_UPDATED_DESCRIPTION_REMOVED
              "location" -> TASK_ACTIVITY_UPDATED_LOCATION_REMOVED
              "workAreaIdentifier" -> TASK_ACTIVITY_REMOVED_WORKAREA
              else -> throw IllegalStateException("Unknown task attribute: $attribute")
            }
      }

  private fun differenceToPrevious(taskAggregateAvro: TaskAggregateAvro): Set<AttributeChange> {
    val taskIdentifier = taskAggregateAvro.getAggregateIdentifier().getIdentifier().toUUID()
    val taskVersion = taskAggregateAvro.getAggregateIdentifier().getVersion()
    val projectIdentifier = taskAggregateAvro.getProject().getIdentifier().toUUID()
    val currentVersion = taskService.find(taskIdentifier, taskVersion, projectIdentifier)
    val previousVersion = taskService.find(taskIdentifier, taskVersion - 1, projectIdentifier)

    val changedAttributes = aggregateComparator.compare(currentVersion, previousVersion)

    return changedAttributes.values.toSet()
  }
}
