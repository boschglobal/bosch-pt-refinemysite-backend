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
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.message.message.getTopicIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.message.facade.listener.message.toEntity
import com.bosch.pt.csm.cloud.projectmanagement.project.message.service.MessageService
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.service.TopicService
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class UpdateStateFromMessageEvent(
    private val topicService: TopicService,
    private val messageService: MessageService
) : AbstractStateStrategy<MessageEventAvro>(), UpdateStateStrategy {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is MessageEventAvro && value.getName() != DELETED

  @Trace
  override fun updateState(key: EventMessageKey, event: MessageEventAvro): Unit =
      event.getAggregate().run {
        val projectIdentifier = key.rootContextIdentifier
        val taskIdentifier =
            topicService.findLatest(getTopicIdentifier(), projectIdentifier).taskIdentifier

        messageService.save(toEntity(taskIdentifier, projectIdentifier))
      }
}
