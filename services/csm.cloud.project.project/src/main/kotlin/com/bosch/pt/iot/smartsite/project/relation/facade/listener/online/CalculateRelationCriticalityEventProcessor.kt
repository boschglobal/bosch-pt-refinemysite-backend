/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.facade.listener.online

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.businesstransaction.facade.listener.BusinessTransactionAware
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.milestone.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message.getTaskIdentifier
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.relation.boundary.RelationCriticalityService
import com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class CalculateRelationCriticalityEventProcessor(
    private val relationCriticalityService: RelationCriticalityService,
) : BusinessTransactionAware {

  override fun getProcessorName() = "relation-criticality"

  @Transactional
  override fun onTransactionFinished(
      transactionStartedRecord: EventRecord,
      events: List<EventRecord>,
      transactionFinishedRecord: EventRecord
  ) = recalculateCriticalityFromEvents(events)

  @Transactional
  override fun onNonTransactionalEvent(record: EventRecord) =
      recalculateCriticalityFromEvents(listOf(record))

  private fun recalculateCriticalityFromEvents(events: List<EventRecord>) =
      events.forEach { (key, event) ->
        when (event) {
          is TaskScheduleEventAvro -> recalculateCriticalityOnScheduleEvent(key, event)
          is MilestoneEventAvro -> recalculateCriticalityOnMilestoneEvent(key, event)
          is RelationEventAvro -> recalculateCriticalityOnRelationEvent(event)
        }
      }

  private fun recalculateCriticalityOnScheduleEvent(
      key: EventMessageKey,
      event: TaskScheduleEventAvro
  ) {
    if (event.getName() != TaskScheduleEventEnumAvro.CREATED &&
        event.getName() != TaskScheduleEventEnumAvro.UPDATED) {
      return
    }
    relationCriticalityService.calculateCriticalityByRelationElements(
        setOf(event.toRelationElementDto()), key.rootContextIdentifier.asProjectId())
  }

  private fun recalculateCriticalityOnMilestoneEvent(
      key: EventMessageKey,
      event: MilestoneEventAvro
  ) {
    if (event.getName() != MilestoneEventEnumAvro.UPDATED) {
      return
    }
    relationCriticalityService.calculateCriticalityByRelationElements(
        setOf(event.toRelationElementDto()), key.rootContextIdentifier.asProjectId())
  }

  private fun recalculateCriticalityOnRelationEvent(event: RelationEventAvro) {
    if (event.getName() != RelationEventEnumAvro.CREATED) {
      return
    }
    relationCriticalityService.calculateCriticalityByRelations(
        setOf(event.getAggregate().getIdentifier()))
  }

  private fun TaskScheduleEventAvro.toRelationElementDto() =
      RelationDto.RelationElementDto(
          getAggregate().getTaskIdentifier(), RelationElementTypeEnum.TASK)

  private fun MilestoneEventAvro.toRelationElementDto() =
      RelationDto.RelationElementDto(
          getAggregate().getIdentifier(), RelationElementTypeEnum.MILESTONE)
}
