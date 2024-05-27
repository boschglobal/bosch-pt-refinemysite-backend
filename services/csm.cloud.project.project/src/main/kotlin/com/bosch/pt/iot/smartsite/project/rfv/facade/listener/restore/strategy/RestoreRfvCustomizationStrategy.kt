/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.rfv.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.streamable.restoredb.DetachedEntityUpdateCallback
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.RFVCUSTOMIZATION
import com.bosch.pt.csm.cloud.projectmanagement.rfv.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.rfv.message.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.facade.listener.restore.strategy.AbstractRestoreStrategy
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextRestoreDbStrategy
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.rfv.model.RfvCustomization
import com.bosch.pt.iot.smartsite.project.rfv.repository.RfvCustomizationRepository
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import jakarta.persistence.EntityManager
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("restore-db", "test")
@Component
open class RestoreRfvCustomizationStrategy(
    private val rfvCustomizationRepository: RfvCustomizationRepository,
    private val projectRepository: ProjectRepository,
    userRepository: UserRepository,
    entityManager: EntityManager
) :
    AbstractRestoreStrategy(entityManager, userRepository, rfvCustomizationRepository),
    ProjectContextRestoreDbStrategy {

  override fun canHandle(
      record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
  ): Boolean =
      RFVCUSTOMIZATION.value == record.key().aggregateIdentifier.type &&
          record.value() is RfvCustomizationEventAvro?

  override fun doHandle(record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>) {
    val key = record.key()
    val event = record.value() as RfvCustomizationEventAvro?
    assertEventNotNull(event, key)

    when (event!!.name) {
      CREATED,
      UPDATED -> {
        val aggregate = event.aggregate

        when (val rfv = findRfv(aggregate.getIdentifier())) {
          null -> createRfvCustomization(aggregate)
          else -> updateRfvCustomization(rfv, aggregate)
        }
      }
      DELETED -> {
        delete(rfvCustomizationRepository.findOneByIdentifier(event.aggregate.getIdentifier()))
      }
      else -> handleInvalidEventType(event.name.name)
    }
  }

  private fun createRfvCustomization(aggregate: RfvCustomizationAggregateAvro) {
    val rfv = RfvCustomization.newInstance()

    setAttributes(rfv, aggregate)
    setAuditAttributes(rfv, aggregate.auditingInformation)

    entityManager.persist(rfv)
  }

  private fun updateRfvCustomization(
      rfvCustomization: RfvCustomization,
      aggregate: RfvCustomizationAggregateAvro
  ) {
    update(
        rfvCustomization,
        object : DetachedEntityUpdateCallback<RfvCustomization> {
          override fun update(entity: RfvCustomization) {
            setAttributes(entity, aggregate)
            setAuditAttributes(entity, aggregate.auditingInformation)
          }
        })
  }

  private fun setAttributes(
      rfvCustomization: RfvCustomization,
      aggregate: RfvCustomizationAggregateAvro
  ) {
    rfvCustomization.identifier = aggregate.getIdentifier()
    rfvCustomization.version = aggregate.getVersion()
    rfvCustomization.active = aggregate.active
    rfvCustomization.name = aggregate.name
    rfvCustomization.project = findProject(aggregate.project)
    rfvCustomization.key = DayCardReasonEnum.valueOf(aggregate.key.name)
  }

  private fun findRfv(identifier: UUID): RfvCustomization? =
      rfvCustomizationRepository.findOneWithDetailsByIdentifier(identifier)

  private fun findProject(aggregateIdentifier: AggregateIdentifierAvro): Project =
      requireNotNull(
          projectRepository.findOneByIdentifier(aggregateIdentifier.identifier.asProjectId())) {
            "Project missing: ${aggregateIdentifier.identifier}"
          }
}
