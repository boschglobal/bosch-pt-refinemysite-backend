/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.boundary.DayCardService
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class CleanupStateFromDayCardDeletedEvent(private val dayCardService: DayCardService) :
    AbstractStateStrategy<DayCardEventG2Avro>(), CleanUpStateStrategy {

  override fun handles(record: EventRecord) =
      record.value is DayCardEventG2Avro &&
          (record.value as DayCardEventG2Avro).name == DayCardEventEnumAvro.DELETED

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: DayCardEventG2Avro) =
      dayCardService.deleteDayCard(event.getIdentifier(), messageKey.rootContextIdentifier)
}
