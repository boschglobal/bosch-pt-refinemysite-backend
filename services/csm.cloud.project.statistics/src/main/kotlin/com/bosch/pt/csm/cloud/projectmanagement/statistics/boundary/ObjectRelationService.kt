/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.AggregateType.EMPLOYEE
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.AggregateType.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.AggregateType.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.AggregateType.PROJECTCRAFT
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.AggregateType.TASK
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.ObjectRelation
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.ObjectRelationRepository
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ObjectRelationService(private val objectRelationRepository: ObjectRelationRepository) {

  @Trace
  @Transactional
  fun saveEmployeeToCompanyRelation(event: EmployeeEventAvro) =
      saveOrUpdate(event.getAggregate().getAggregateIdentifier(), event.getAggregate().getCompany())

  @Trace
  @Transactional
  fun saveEmployeeToUserRelation(event: EmployeeEventAvro) =
      saveOrUpdate(event.getAggregate().getAggregateIdentifier(), event.getAggregate().getUser())

  @Trace
  fun saveParticipantToUserRelation(event: ParticipantEventG3Avro) =
      saveOrUpdate(event.getAggregate().getAggregateIdentifier(), event.getAggregate().getUser())

  @Trace
  fun saveParticipantToCompanyRelation(event: ParticipantEventG3Avro) =
      saveOrUpdate(event.getAggregate().getAggregateIdentifier(), event.getAggregate().getCompany())

  @Trace
  fun saveParticipantToProjectRelation(event: ParticipantEventG3Avro) =
      saveOrUpdate(event.getAggregate().getAggregateIdentifier(), event.getAggregate().getProject())

  @Trace
  @Transactional
  fun saveCraftToProjectRelation(event: ProjectCraftEventG2Avro) =
      saveOrUpdate(event.getAggregate().getAggregateIdentifier(), event.getAggregate().getProject())

  @Trace
  @Transactional(readOnly = true)
  fun findAssignedParticipantForTask(task: AggregateIdentifierAvro) =
      objectRelationRepository.findOneByLeftAndRightType(ObjectIdentifier(task), PARTICIPANT)?.right

  @Trace
  @Transactional(readOnly = true)
  fun findProjectCraftForTask(task: AggregateIdentifierAvro) =
      objectRelationRepository
          .findOneByLeftAndRightType(ObjectIdentifier(task), PROJECTCRAFT)
          ?.right
          ?: throw IllegalStateException("Craft not found for task ${task.getIdentifier()}")

  @Trace
  @Transactional(readOnly = true)
  fun findProjectByTask(taskIdentifier: ObjectIdentifier): ObjectIdentifier =
      objectRelationRepository.findOneByLeftAndRightType(taskIdentifier, PROJECT)?.right
          ?: throw IllegalStateException("Project not found for task ${taskIdentifier.identifier}")

  @Trace
  @Transactional
  fun saveTaskToAssignedParticipantRelation(event: TaskEventAvro) =
      objectRelationRepository.save(
          objectRelationRepository
              .findOneByLeftAndRightType(
                  ObjectIdentifier(event.getAggregate().getAggregateIdentifier()), PARTICIPANT)
              ?.also { it.right = ObjectIdentifier(event.getAggregate().getAssignee()) }
              ?: fromAggregate(
                  event.getAggregate().getAggregateIdentifier(),
                  event.getAggregate().getAssignee()))

  @Trace
  @Transactional
  fun deleteTaskToAssignedParticipantRelation(event: TaskEventAvro) {
    val taskAssigneeRelation =
        objectRelationRepository.findOneByLeftAndRightType(
            ObjectIdentifier(event.getAggregate().getAggregateIdentifier()), PARTICIPANT)
    if (taskAssigneeRelation != null) {
      objectRelationRepository.delete(taskAssigneeRelation)
    }
  }

  @Trace
  @Transactional
  fun saveTaskToCraftRelation(event: TaskEventAvro): ObjectRelation =
      objectRelationRepository.save(
          objectRelationRepository
              .findOneByLeftAndRightType(
                  ObjectIdentifier(event.getAggregate().getAggregateIdentifier()), PROJECTCRAFT)
              ?.also { it.right = ObjectIdentifier(event.getAggregate().getCraft()) }
              ?: fromAggregate(
                  event.getAggregate().getAggregateIdentifier(), event.getAggregate().getCraft()))

  @Trace
  @Transactional
  fun saveTaskToProjectRelation(event: TaskEventAvro): ObjectRelation =
      objectRelationRepository.save(
          objectRelationRepository
              .findOneByLeftAndRightType(
                  ObjectIdentifier(event.getAggregate().getAggregateIdentifier()), PROJECT)
              ?.also { it.right = ObjectIdentifier(event.getAggregate().getProject()) }
              ?: fromAggregate(
                  event.getAggregate().getAggregateIdentifier(), event.getAggregate().getProject()))

  @Trace
  @Transactional(readOnly = true)
  fun findAllByLeftTypeAndRight(type: String, objectIdentifier: ObjectIdentifier) =
      getLeftObjectIdentifiers(
          objectRelationRepository.findAllByLeftTypeAndRight(type, objectIdentifier))

  @Trace
  @Transactional
  fun deleteByProjectIdentifier(projectIdentifier: UUID) {
    val idsToDelete: MutableList<Long> = ArrayList()

    val taskProjectRelations =
        objectRelationRepository.findAllByLeftTypeAndRight(
            TASK, ObjectIdentifier(PROJECT, projectIdentifier))
    idsToDelete.addAll(getIdsOfRelations(taskProjectRelations))

    val participantProjectRelations =
        objectRelationRepository.findAllByLeftTypeAndRight(
            PARTICIPANT, ObjectIdentifier(PROJECT, projectIdentifier))
    idsToDelete.addAll(getIdsOfRelations(participantProjectRelations))

    idsToDelete.addAll(
        getIdsOfRelations(
            objectRelationRepository.findAllByLeftTypeAndRight(
                PROJECTCRAFT, ObjectIdentifier(PROJECT, projectIdentifier))))

    idsToDelete.addAll(
        getIdsOfRelations(
            objectRelationRepository.findAllByLeftInAndRightType(
                getLeftObjectIdentifiers(participantProjectRelations), EMPLOYEE)))

    idsToDelete.addAll(
        getIdsOfRelations(
            objectRelationRepository.findAllByLeftInAndRightType(
                getLeftObjectIdentifiers(taskProjectRelations), PROJECTCRAFT)))

    idsToDelete.addAll(
        getIdsOfRelations(
            objectRelationRepository.findAllByLeftInAndRightType(
                getLeftObjectIdentifiers(taskProjectRelations), PARTICIPANT)))

    if (idsToDelete.isNotEmpty()) {
      objectRelationRepository.deleteAll(idsToDelete)
    }
  }

  @Trace
  @Transactional
  fun deleteByTaskIdentifier(taskIdentifier: UUID) {
    val idsToDelete: MutableList<Long> = ArrayList()

    val taskObjectIdentifier = ObjectIdentifier(TASK, taskIdentifier)

    objectRelationRepository.findOneByLeftAndRightType(taskObjectIdentifier, PROJECT)?.let {
      idsToDelete.add(it.id!!)
    }

    objectRelationRepository.findOneByLeftAndRightType(taskObjectIdentifier, PROJECTCRAFT)?.let {
      idsToDelete.add(it.id!!)
    }

    objectRelationRepository.findOneByLeftAndRightType(taskObjectIdentifier, PARTICIPANT)?.let {
      idsToDelete.add(it.id!!)
    }

    if (idsToDelete.isNotEmpty()) {
      objectRelationRepository.deleteAll(idsToDelete)
    }
  }

  private fun getLeftObjectIdentifiers(relations: List<ObjectRelation>): List<ObjectIdentifier> =
      relations.map(ObjectRelation::left)

  private fun getIdsOfRelations(relations: List<ObjectRelation>) = relations.map { it.id!! }

  private fun saveOrUpdate(left: AggregateIdentifierAvro, right: AggregateIdentifierAvro) {
    val newRelation = fromAggregate(left, right)
    objectRelationRepository
        .findOneByLeftAndRightType(newRelation.left, newRelation.right.type)
        ?.also { it.right = newRelation.right }
        ?: objectRelationRepository.save(newRelation)
  }

  private fun fromAggregate(
      left: AggregateIdentifierAvro,
      right: AggregateIdentifierAvro
  ): ObjectRelation = fromAggregate(left, ObjectIdentifier(right))

  private fun fromAggregate(left: AggregateIdentifierAvro, right: ObjectIdentifier) =
      ObjectRelation(ObjectIdentifier(left), right)
}
