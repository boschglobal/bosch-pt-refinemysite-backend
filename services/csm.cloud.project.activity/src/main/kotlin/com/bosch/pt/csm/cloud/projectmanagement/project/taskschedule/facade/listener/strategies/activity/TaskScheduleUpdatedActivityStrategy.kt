/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2021
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.strategies.activity

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Activity
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.AttributeChanges
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ChangeDescription
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Details
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.SimpleDate
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.SimpleString
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Summary
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Value
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.activity.AbstractActivityStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_CREATED_END
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_CREATED_START
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_DELETED_END
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_DELETED_START
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_UPDATED_DAY_CARD_ADDED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_UPDATED_DAY_CARD_REMOVED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_UPDATED_DAY_CARD_REORDERED
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_UPDATED_END
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.TASK_SCHEDULE_ACTIVITY_UPDATED_START
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.activity.AttributeChangeEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.service.DayCardService
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.service.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.message.buildContext
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.message.buildEventInformation
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.service.TaskScheduleService
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getLastModifiedByUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getVersion
import datadog.trace.api.Trace
import java.time.LocalDate
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component
import org.springframework.util.IdGenerator

@Component
class TaskScheduleUpdatedActivityStrategy(
    private val participantService: ParticipantService,
    private val taskScheduleService: TaskScheduleService,
    private val dayCardService: DayCardService,
    private val aggregateComparator: TaskScheduleAggregateComparator,
    private val idGenerator: IdGenerator
) : AbstractActivityStrategy<TaskScheduleEventAvro>() {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is TaskScheduleEventAvro && value.getName() == UPDATED

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
        templateMessageKey = TASK_SCHEDULE_ACTIVITY_UPDATED,
        references = mapOf("originator" to originator))
  }

  private fun buildDetails(projectIdentifier: UUID, aggregate: TaskScheduleAggregateAvro): Details {

    val changedAttributes = differenceToPrevious(projectIdentifier, aggregate)

    return AttributeChanges(
        changedAttributes.map {
          ChangeDescription(
              mapToMessageKey(it.attribute, it.changeType),
              mapToMessageValues(it, projectIdentifier))
        })
  }

  private fun differenceToPrevious(
      projectIdentifier: UUID,
      aggregate: TaskScheduleAggregateAvro
  ): Collection<TaskScheduleAttributeChange> {

    val identifier = aggregate.getIdentifier()
    val version = aggregate.getVersion()
    val currentVersion = taskScheduleService.find(identifier, version, projectIdentifier)
    val previousVersion = taskScheduleService.find(identifier, version - 1, projectIdentifier)

    return aggregateComparator.compare(currentVersion, previousVersion)
  }

  private fun mapToMessageKey(attribute: String, changeType: AttributeChangeEnum) =
      when (changeType) {
        AttributeChangeEnum.CREATED ->
            when (attribute) {
              "start" -> TASK_SCHEDULE_ACTIVITY_CREATED_START
              "end" -> TASK_SCHEDULE_ACTIVITY_CREATED_END
              "daycard" -> TASK_SCHEDULE_ACTIVITY_UPDATED_DAY_CARD_ADDED
              else ->
                  throw IllegalStateException("Unknown created task schedule attribute: $attribute")
            }
        AttributeChangeEnum.UPDATED ->
            when (attribute) {
              "start" -> TASK_SCHEDULE_ACTIVITY_UPDATED_START
              "end" -> TASK_SCHEDULE_ACTIVITY_UPDATED_END
              "daycard" -> TASK_SCHEDULE_ACTIVITY_UPDATED_DAY_CARD_REORDERED
              else ->
                  throw IllegalStateException("Unknown updated task schedule attribute: $attribute")
            }
        AttributeChangeEnum.REMOVED ->
            when (attribute) {
              "start" -> TASK_SCHEDULE_ACTIVITY_DELETED_START
              "end" -> TASK_SCHEDULE_ACTIVITY_DELETED_END
              "daycard" -> TASK_SCHEDULE_ACTIVITY_UPDATED_DAY_CARD_REMOVED
              else ->
                  throw IllegalStateException("Unknown removed task schedule attribute: $attribute")
            }
      }

  private fun mapToMessageValues(attribute: TaskScheduleAttributeChange, projectIdentifier: UUID) =
      when (attribute.attribute) {
        "daycard" -> buildDayCardAttributeValues(attribute, projectIdentifier)
        "start" -> buildLocalDatesAttributeValues(attribute)
        "end" -> buildLocalDatesAttributeValues(attribute)
        else -> buildDefaultAttributeValues(attribute)
      }

  private fun buildDayCardAttributeValues(
      attribute: TaskScheduleAttributeChange,
      projectIdentifier: UUID
  ): List<Value> {
    val dayCardTitle =
        SimpleString(
            dayCardService.findLatest(
                    identifier = attribute.attributeIdentifier!!,
                    projectIdentifier = projectIdentifier)
                .title)

    val values =
        listOfNotNull(attribute.oldValue, attribute.newValue).map { SimpleDate(it as LocalDate) }

    return listOfNotNull(dayCardTitle).plus(values)
  }

  private fun buildLocalDatesAttributeValues(attribute: TaskScheduleAttributeChange) =
      listOfNotNull(attribute.oldValue, attribute.newValue).map { SimpleDate(it as LocalDate) }

  private fun buildDefaultAttributeValues(attribute: TaskScheduleAttributeChange) =
      listOfNotNull(attribute.oldValue, attribute.newValue).map { SimpleString(it as String) }
}
