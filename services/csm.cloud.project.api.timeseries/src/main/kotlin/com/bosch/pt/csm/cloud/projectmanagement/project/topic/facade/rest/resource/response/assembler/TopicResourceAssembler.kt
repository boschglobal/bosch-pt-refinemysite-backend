/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.rest.resource.response.TopicResource
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.model.Topic
import org.springframework.stereotype.Component

@Component
class TopicResourceAssembler {

  fun assemble(topic: Topic, latestOnly: Boolean): List<TopicResource> =
      if (latestOnly) {
        listOf(
            TopicResourceMapper.INSTANCE.fromTopicVersion(
                topic.history.last(), topic.project, topic.task, topic.identifier))
      } else {
        topic.history.map {
          TopicResourceMapper.INSTANCE.fromTopicVersion(
              it, topic.project, topic.task, topic.identifier)
        }
      }
}
