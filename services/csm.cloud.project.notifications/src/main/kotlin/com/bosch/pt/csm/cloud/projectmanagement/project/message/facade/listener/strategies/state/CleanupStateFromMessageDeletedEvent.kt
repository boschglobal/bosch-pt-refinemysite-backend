/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.message.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.message.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.message.boundary.MessageService
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class CleanupStateFromMessageDeletedEvent(private val messageService: MessageService) :
    AbstractStateStrategy<MessageEventAvro>(), CleanUpStateStrategy {

  override fun handles(record: EventRecord): Boolean {
    return record.value is MessageEventAvro &&
        (record.value as MessageEventAvro).name == MessageEventEnumAvro.DELETED
  }

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: MessageEventAvro) =
      messageService.deleteMessage(event.getIdentifier(), messageKey.rootContextIdentifier)
}
