/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.listener.message.toEntity
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.service.RfvCustomizationService
import com.bosch.pt.csm.cloud.projectmanagement.rfv.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.DELETED
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class UpdateStateFromRfvCustomizationEvent(
    private val rfvCustomizationService: RfvCustomizationService
) : AbstractStateStrategy<RfvCustomizationEventAvro>(), UpdateStateStrategy {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is RfvCustomizationEventAvro && value.name != DELETED

  @Trace
  override fun updateState(key: EventMessageKey, event: RfvCustomizationEventAvro) =
      event.aggregate.run {
        val projectIdentifier = key.rootContextIdentifier

        rfvCustomizationService.save(toEntity())
        rfvCustomizationService.deleteByVersion(
            getIdentifier(), aggregateIdentifier.version - 1, projectIdentifier)
      }
}
