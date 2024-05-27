/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.message.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.message.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.message.service.MessageService
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class CleanupStateFromMessageDeletedEvent(private val messageService: MessageService) :
    AbstractStateStrategy<MessageEventAvro>(), CleanUpStateStrategy {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is MessageEventAvro && value.getName() == DELETED

  @Trace
  override fun updateState(key: EventMessageKey, event: MessageEventAvro) =
      messageService.delete(event.getIdentifier(), key.rootContextIdentifier)
}
