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
import com.bosch.pt.csm.cloud.projectmanagement.message.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.message.getTopicIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.message.boundary.MessageService
import com.bosch.pt.csm.cloud.projectmanagement.project.message.model.Message
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.boundary.TopicService
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class UpdateStateFromMessageEvent(
    private val topicService: TopicService,
    private val messageService: MessageService
) : AbstractStateStrategy<MessageEventAvro>(), UpdateStateStrategy {

  override fun handles(record: EventRecord) =
      record.value is MessageEventAvro &&
          (record.value as MessageEventAvro).name != MessageEventEnumAvro.DELETED

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: MessageEventAvro): Unit =
      event.aggregate.run {
        val projectIdentifier = messageKey.rootContextIdentifier
        val taskIdentifier =
            topicService.findLatest(getTopicIdentifier(), projectIdentifier).taskIdentifier

        messageService.save(
            Message(
                identifier = buildAggregateIdentifier(),
                projectIdentifier = projectIdentifier,
                taskIdentifier = taskIdentifier,
                topicIdentifier = getTopicIdentifier(),
                content = content,
            ))
      }
}
