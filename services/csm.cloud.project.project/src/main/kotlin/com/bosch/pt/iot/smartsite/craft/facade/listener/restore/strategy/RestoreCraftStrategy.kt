/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.craft.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.referencedata.craft.common.CraftAggregateTypeEnum.CRAFT
import com.bosch.pt.csm.cloud.referencedata.craft.CraftAggregateAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftEventAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftEventEnumAvro.CREATED
import com.bosch.pt.iot.smartsite.common.facade.listener.restore.strategy.AbstractRestoreStrategy
import com.bosch.pt.iot.smartsite.common.model.Translation
import com.bosch.pt.iot.smartsite.craft.model.Craft
import com.bosch.pt.iot.smartsite.craft.repository.CraftRepository
import com.bosch.pt.iot.smartsite.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("restore-db", "test")
@Component
open class RestoreCraftStrategy(
    private val craftRepository: CraftRepository,
    userRepository: UserRepository,
    entityManager: EntityManager
) :
    AbstractRestoreStrategy(entityManager, userRepository, craftRepository),
    CraftContextRestoreDbStrategy {

  override fun canHandle(
      record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>
  ): Boolean =
      CRAFT.value == record.key().aggregateIdentifier.type && record.value() is CraftEventAvro?

  override fun doHandle(record: ConsumerRecord<AggregateEventMessageKey, SpecificRecordBase?>) {
    val event = record.value() as CraftEventAvro?
    assertEventNotNull(event, record.key())

    if (event!!.getName() == CREATED) {
      createCraft(event.getAggregate())
    } else {
      handleInvalidEventType(event.getName().name)
    }
  }

  private fun createCraft(aggregate: CraftAggregateAvro) =
      craftRepository.findOneByIdentifier(
          aggregate.getAggregateIdentifier().getIdentifier().toUUID())
          ?: run {
            val translations =
                aggregate
                    .getTranslations()
                    .map { Translation(it.getLocale(), it.getValue()) }
                    .toSet()

            val craft =
                Craft(
                        aggregate.getAggregateIdentifier().getIdentifier().toUUID(),
                        aggregate.getAggregateIdentifier().getVersion(),
                        aggregate.getDefaultName(),
                        translations)
                    .apply { setAuditAttributes(this, aggregate.getAuditingInformation()) }

            entityManager.persist(craft)
          }
}
