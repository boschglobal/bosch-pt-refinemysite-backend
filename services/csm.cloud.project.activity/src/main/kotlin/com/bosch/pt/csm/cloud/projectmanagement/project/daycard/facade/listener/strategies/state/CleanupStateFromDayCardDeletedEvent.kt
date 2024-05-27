/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.service.DayCardService
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class CleanupStateFromDayCardDeletedEvent(private val dayCardService: DayCardService) :
    AbstractStateStrategy<DayCardEventG2Avro>(), CleanUpStateStrategy {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is DayCardEventG2Avro && value.getName() == DELETED

  @Trace
  override fun updateState(key: EventMessageKey, event: DayCardEventG2Avro) =
      dayCardService.delete(event.getIdentifier(), key.rootContextIdentifier)
}
