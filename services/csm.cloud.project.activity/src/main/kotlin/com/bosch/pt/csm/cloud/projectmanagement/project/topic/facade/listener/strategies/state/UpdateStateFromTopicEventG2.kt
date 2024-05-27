/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.listener.message.toEntity
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.service.TopicService
import com.bosch.pt.csm.cloud.projectmanagement.topic.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventG2Avro
import datadog.trace.api.Trace
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class UpdateStateFromTopicEventG2(private val topicService: TopicService) :
    AbstractStateStrategy<TopicEventG2Avro>(), UpdateStateStrategy {

  override fun handles(key: EventMessageKey, value: SpecificRecordBase?): Boolean =
      value is TopicEventG2Avro && value.getName() != DELETED

  @Trace
  override fun updateState(key: EventMessageKey, event: TopicEventG2Avro) =
      event.getAggregate().run {
        val projectIdentifier = key.rootContextIdentifier

        topicService.save(toEntity(projectIdentifier))
        topicService.deleteByVersion(
            getIdentifier(), getAggregateIdentifier().getVersion() - 2, projectIdentifier)
      }
}
