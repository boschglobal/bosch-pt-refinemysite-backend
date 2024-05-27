/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.listener

import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.rfv.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.DayCardService
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.NamedObjectService
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.ObjectRelationService
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.ParticipantMappingService
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.RfvCustomizationService
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.AggregateType
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.util.toUUID
import jakarta.transaction.Transactional
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class ProjectEventToStateProcessor(
    private val dayCardService: DayCardService,
    private val namedObjectService: NamedObjectService,
    private val objectRelationService: ObjectRelationService,
    private val participantMappingService: ParticipantMappingService,
    private val rfvCustomizationService: RfvCustomizationService
) {

  @Transactional
  fun updateStateFromProjectEvent(message: SpecificRecordBase?) {
    when {
      message is DayCardEventG2Avro -> updateFromDayCardEvent(message)
      message is ParticipantEventG3Avro &&
          message.getName() != ParticipantEventEnumAvro.CANCELLED &&
          message.getAggregate().getStatus() != ParticipantStatusEnumAvro.VALIDATION &&
          message.getAggregate().getStatus() != ParticipantStatusEnumAvro.INVITED ->
          updateFromParticipantEvent(message)
      message is ProjectEventAvro -> updateFromProjectEvent(message)
      message is ProjectCraftEventG2Avro -> updateFromProjectCraftEvent(message)
      message is TaskEventAvro -> updateFromTaskEvent(message)
      message is TaskScheduleEventAvro -> updateFromTaskScheduleEvent(message)
      message is RfvCustomizationEventAvro -> updateFromRfvEvent(message)
    }
  }

  private fun updateFromDayCardEvent(event: DayCardEventG2Avro) {
    when (DayCardEventEnumAvro.DELETED) {
      event.getName() -> dayCardService.deleteDayCard(event)
      else -> dayCardService.saveDayCard(event)
    }
  }

  private fun updateFromProjectEvent(event: ProjectEventAvro) {
    if (event.getName() == ProjectEventEnumAvro.DELETED) {

      val projectIdentifier = event.getAggregate().getAggregateIdentifier().toUUID()

      // delete named objects
      val projectCraftIdentifiers =
          objectRelationService.findAllByLeftTypeAndRight(
              AggregateType.PROJECTCRAFT,
              ObjectIdentifier(event.getAggregate().getAggregateIdentifier()))

      namedObjectService.deleteAll(projectCraftIdentifiers)

      // delete day cards
      dayCardService.deleteByProjectIdentifier(projectIdentifier)

      // delete participant mappings
      participantMappingService.deleteAllByProjectIdentifier(projectIdentifier)

      // delete object relations
      objectRelationService.deleteByProjectIdentifier(projectIdentifier)
    }
  }

  private fun updateFromParticipantEvent(event: ParticipantEventG3Avro) {
    objectRelationService.saveParticipantToUserRelation(event)
    objectRelationService.saveParticipantToCompanyRelation(event)
    objectRelationService.saveParticipantToProjectRelation(event)
    participantMappingService.saveParticipant(event)
  }

  private fun updateFromProjectCraftEvent(event: ProjectCraftEventG2Avro) {
    objectRelationService.saveCraftToProjectRelation(event)
    namedObjectService.saveProjectCraftName(event)
  }

  private fun updateFromTaskEvent(event: TaskEventAvro) {

    if (event.getName() == TaskEventEnumAvro.DELETED) {

      val taskIdentifier = event.getAggregate().getAggregateIdentifier().toUUID()

      dayCardService.deleteByTaskIdentifier(taskIdentifier)
      objectRelationService.deleteByTaskIdentifier(taskIdentifier)
    } else if (event.getName() == TaskEventEnumAvro.UNASSIGNED) {
      // Remove relation between task and participant in case of UNASSIGNED
      objectRelationService.deleteTaskToAssignedParticipantRelation(event)
      dayCardService.updateDayCardFromTaskEvent(event)
    } else {
      if (event.getAggregate().getAssignee() != null) {
        objectRelationService.saveTaskToAssignedParticipantRelation(event)
      }
      objectRelationService.saveTaskToCraftRelation(event)
      objectRelationService.saveTaskToProjectRelation(event)
      dayCardService.updateDayCardFromTaskEvent(event)
    }
  }

  private fun updateFromTaskScheduleEvent(event: TaskScheduleEventAvro) {
    dayCardService.updateDayCardFromTaskScheduleEvent(event)
  }

  private fun updateFromRfvEvent(event: RfvCustomizationEventAvro) {
    if (event.getName() == RfvCustomizationEventEnumAvro.DELETED) {
      rfvCustomizationService.deleteRfvCustomization(event.getIdentifier())
    } else {
      rfvCustomizationService.createOrUpdateRfvCustomizationFromEvent(event)
    }
  }
}
