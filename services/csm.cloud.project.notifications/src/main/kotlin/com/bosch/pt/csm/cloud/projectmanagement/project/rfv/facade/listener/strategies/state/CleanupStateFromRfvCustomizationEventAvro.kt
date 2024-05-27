/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.boundary.RfvCustomizationService
import com.bosch.pt.csm.cloud.projectmanagement.rfv.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.DELETED
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class CleanupStateFromRfvCustomizationEventAvro(
    private val rfvCustomizationService: RfvCustomizationService
) : AbstractStateStrategy<RfvCustomizationEventAvro>(), CleanUpStateStrategy {

  override fun handles(record: EventRecord) =
      record.value.run { this is RfvCustomizationEventAvro && this.name == DELETED }

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: RfvCustomizationEventAvro) =
      rfvCustomizationService.delete(event.getIdentifier(), messageKey.rootContextIdentifier)
}
