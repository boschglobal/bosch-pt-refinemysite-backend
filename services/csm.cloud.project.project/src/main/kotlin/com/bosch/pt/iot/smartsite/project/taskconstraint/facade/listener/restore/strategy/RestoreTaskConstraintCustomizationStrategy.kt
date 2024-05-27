/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskconstraint.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.streamable.restoredb.DetachedEntityUpdateCallback
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASKCONSTRAINTCUSTOMIZATION
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.message.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.facade.listener.restore.strategy.AbstractRestoreStrategy
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextRestoreDbStrategy
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintCustomization
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.iot.smartsite.project.taskconstraint.repository.TaskConstraintCustomizationRepository
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import jakarta.persistence.EntityManager
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("restore-db", "test")
@Component
open class RestoreTaskConstraintCustomizationStrategy(
    private val constraintCustomizationRepository: TaskConstraintCustomizationRepository,
    private val projectRepository: ProjectRepository,
    userRepository: UserRepository,
    entityManager: EntityManager
) :
    AbstractRestoreStrategy(entityManager, userRepository, constraintCustomizationRepository),
    ProjectContextRestoreDbStrategy {

  override fun canHandle(
      record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
  ): Boolean =
      TASKCONSTRAINTCUSTOMIZATION.value == record.key().aggregateIdentifier.type &&
          record.value() is TaskConstraintCustomizationEventAvro?

  override fun doHandle(record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>) {
    val key = record.key()
    val event = record.value() as TaskConstraintCustomizationEventAvro?
    assertEventNotNull(event, key)

    when (event!!.getName()) {
      CREATED,
      UPDATED -> {
        val aggregate = event.getAggregate()

        when (val constraintCustomization =
            findConstraintCustomization(aggregate.getIdentifier())) {
          null -> createConstraintCustomization(aggregate)
          else -> updateConstraintCustomization(constraintCustomization, aggregate)
        }
      }
      DELETED -> {
        delete(
            constraintCustomizationRepository.findOneByIdentifier(
                event.getAggregate().getIdentifier()))
      }
      else -> handleInvalidEventType(event.getName().name)
    }
  }

  private fun createConstraintCustomization(aggregate: TaskConstraintCustomizationAggregateAvro) {
    val constraint = TaskConstraintCustomization.newInstance()

    setAttributes(constraint, aggregate)
    setAuditAttributes(constraint, aggregate.auditingInformation)

    entityManager.persist(constraint)
  }

  private fun updateConstraintCustomization(
      constraintCustomization: TaskConstraintCustomization,
      aggregate: TaskConstraintCustomizationAggregateAvro
  ) {
    update(
        constraintCustomization,
        object : DetachedEntityUpdateCallback<TaskConstraintCustomization> {
          override fun update(entity: TaskConstraintCustomization) {
            setAttributes(entity, aggregate)
            setAuditAttributes(entity, aggregate.auditingInformation)
          }
        })
  }

  private fun setAttributes(
      constraintCustomization: TaskConstraintCustomization,
      aggregate: TaskConstraintCustomizationAggregateAvro
  ) {
    constraintCustomization.identifier = aggregate.getIdentifier()
    constraintCustomization.version = aggregate.getVersion()
    constraintCustomization.active = aggregate.getActive()
    constraintCustomization.name = aggregate.getName()
    constraintCustomization.project = findProject(aggregate.getProject())
    constraintCustomization.key = TaskConstraintEnum.valueOf(aggregate.getKey().name)
  }

  private fun findConstraintCustomization(identifier: UUID): TaskConstraintCustomization? =
      constraintCustomizationRepository.findOneWithDetailsByIdentifier(identifier)

  private fun findProject(aggregateIdentifier: AggregateIdentifierAvro): Project =
      requireNotNull(
          projectRepository.findOneByIdentifier(
              aggregateIdentifier.getIdentifier().asProjectId())) {
            "Project missing: ${aggregateIdentifier.getIdentifier()}"
          }
}
