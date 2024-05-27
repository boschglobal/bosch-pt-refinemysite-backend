/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.boundary.TopicService
import com.bosch.pt.csm.cloud.projectmanagement.topic.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventG2Avro
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class CleanupStateFromTopicDeletedEventG2(private val topicService: TopicService) :
    AbstractStateStrategy<TopicEventG2Avro>(), CleanUpStateStrategy {

  override fun handles(record: EventRecord): Boolean {
    return record.value is TopicEventG2Avro &&
        (record.value as TopicEventG2Avro).name == TopicEventEnumAvro.DELETED
  }

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: TopicEventG2Avro) =
      topicService.deleteTopicAndAllRelatedDocuments(
          event.getIdentifier(), messageKey.rootContextIdentifier)
}
