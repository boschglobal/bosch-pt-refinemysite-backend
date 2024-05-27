/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCard
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardReasonNotDoneEnum
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardStatusEnum
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.DayCardRepository
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.util.toUUID
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DayCardService(
    private val dayCardRepository: DayCardRepository,
    private val objectRelationService: ObjectRelationService,
    private val participantMappingService: ParticipantMappingService
) {

  @Trace
  @Transactional
  fun saveDayCard(event: DayCardEventG2Avro) {
    val aggregate = event.getAggregate()
    saveDayCardInternal(
        aggregate.getTask(),
        aggregate.getAggregateIdentifier(),
        statusOf(aggregate),
        reasonNotDoneFrom(aggregate))
  }

  @Trace
  @Transactional
  fun deleteDayCard(event: DayCardEventG2Avro) {
    val dayCardIdentifier = ObjectIdentifier(event.getAggregate().getAggregateIdentifier())
    dayCardRepository.deleteByContextObjectIdentifier(dayCardIdentifier.identifier)
  }

  @Trace
  @Transactional
  fun updateDayCardFromTaskEvent(event: TaskEventAvro) {

    val assignedParticipant =
        event.getAggregate().getAssignee()?.let {
          participantMappingService.findOneByParticipantIdentifier(it.toUUID())
        }

    val dayCardsOfTask =
        dayCardRepository.findAllByTaskIdentifier(
            event.getAggregate().getAggregateIdentifier().toUUID())

    dayCardsOfTask.forEach { dayCard ->
      dayCard.craftIdentifier = event.getAggregate().getCraft().toUUID()
      dayCard.assignedParticipant = assignedParticipant
    }
    dayCardRepository.saveAll(dayCardsOfTask)
  }

  @Trace
  @Transactional
  fun updateDayCardFromTaskScheduleEvent(event: TaskScheduleEventAvro) {

    val assignedParticipantIdentifier =
        objectRelationService.findAssignedParticipantForTask(event.getAggregate().getTask())

    val assignedParticipant =
        assignedParticipantIdentifier?.let {
          participantMappingService.findOneByParticipantIdentifier(it.identifier)
        }

    val craftIdentifier =
        objectRelationService.findProjectCraftForTask(event.getAggregate().getTask())

    val dayCardMap =
        dayCardRepository.findAllByTaskIdentifier(event.getAggregate().getTask().toUUID())
            .associateBy { it.contextObject.identifier }

    event.getAggregate().getSlots().forEach { slot ->
      val dayCard = dayCardMap[slot.getDayCard().toUUID()]!!
      dayCard.assignedParticipant = assignedParticipant
      dayCard.craftIdentifier = craftIdentifier.identifier
      dayCard.date = slot.getDate().toLocalDateByMillis()
    }

    dayCardRepository.saveAll(dayCardMap.values)
  }

  @Trace
  @Transactional
  fun deleteByProjectIdentifier(projectIdentifier: UUID) {
    dayCardRepository.deleteByProjectIdentifier(projectIdentifier)
  }

  @Trace
  @Transactional
  fun deleteByTaskIdentifier(taskIdentifier: UUID) {
    val dayCardIds = dayCardRepository.findIdsByTaskIdentifier(taskIdentifier)
    dayCardRepository.deleteAll(dayCardIds)
  }

  private fun saveDayCardInternal(
      taskAggregate: AggregateIdentifierAvro,
      aggregateIdentifier: AggregateIdentifierAvro,
      status: DayCardStatusEnum,
      reasonNotDone: DayCardReasonNotDoneEnum?
  ) {
    val taskIdentifier = ObjectIdentifier(taskAggregate)
    val projectIdentifier = objectRelationService.findProjectByTask(taskIdentifier).identifier
    val dayCardIdentifier = ObjectIdentifier(aggregateIdentifier)
    val dayCard =
        dayCardRepository.findByContextObject(dayCardIdentifier)?.also {
          it.reason = reasonNotDone
          it.status = status
        }
            ?: DayCard(
                dayCardIdentifier,
                projectIdentifier,
                status,
                reasonNotDone,
                taskIdentifier.identifier)

    dayCardRepository.save(dayCard)
  }

  private fun statusOf(aggregate: DayCardAggregateG2Avro) =
      DayCardStatusEnum.valueOf(aggregate.getStatus().toString())

  private fun reasonNotDoneFrom(aggregate: DayCardAggregateG2Avro) =
      aggregate.getReason()?.let { DayCardReasonNotDoneEnum.valueOf(it.toString()) }
}
