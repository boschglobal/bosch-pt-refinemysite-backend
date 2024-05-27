/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.craft.craft.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJpa
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.referencedata.craft.CraftAggregateAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftEventAvro
import com.bosch.pt.csm.cloud.referencedata.craft.CraftEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.usermanagement.common.model.Translation
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.CraftId
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.toCraftId
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.model.Craft
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.repository.CraftRepository
import com.bosch.pt.csm.cloud.usermanagement.craft.eventstore.CraftContextSnapshotStore
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class CraftSnapshotStore(val repository: CraftRepository) :
    AbstractSnapshotStoreJpa<CraftEventAvro, CraftSnapshot, Craft, CraftId>(),
    CraftContextSnapshotStore {

  override fun findOrFail(identifier: CraftId): CraftSnapshot {
    throw UnsupportedOperationException()
  }

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase): Boolean =
      key.aggregateIdentifier.type == "CRAFT" &&
          message is CraftEventAvro &&
          message.getName() == CREATED

  override fun updateInternal(event: CraftEventAvro, currentSnapshot: Craft?) =
      when (currentSnapshot == null) {
        true -> createCraft(event.getAggregate())
        false -> error("Craft was found but event was not skipped")
      }

  override fun findInternal(identifier: UUID): Craft? =
      repository.findOneByIdentifier(CraftId(identifier))

  override fun isDeletedEvent(message: SpecificRecordBase) = false

  fun createCraft(aggregate: CraftAggregateAvro): Long {
    val existingCraft = repository.findOneByIdentifier(aggregate.toCraftId())

    return if (existingCraft == null) {
      aggregate
          .getTranslations()
          .map { Translation(it.getLocale(), it.getValue()) }
          .toSet()
          .let {
            Craft(CraftId(aggregate.getIdentifier()), aggregate.getDefaultName(), it).let { craft ->
              craft.version = aggregate.getAggregateIdentifier().getVersion()
              setAuditAttributes(craft, aggregate.getAuditingInformation())
              repository.saveAndFlush(craft).version
            }
          }
    } else {
      existingCraft.version
    }
  }
}
