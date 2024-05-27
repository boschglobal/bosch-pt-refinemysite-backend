/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.rest.resource.response.TopicListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.model.Topic
import org.springframework.stereotype.Component

@Component
class TopicListResourceAssembler(private val topicResourceAssembler: TopicResourceAssembler) {

  fun assemble(topics: List<Topic>, latestOnly: Boolean): TopicListResource =
      TopicListResource(
          topics
              .flatMap { topicResourceAssembler.assemble(it, latestOnly) }
              .sortedWith(compareBy({ it.id.value }, { it.version }, { it.eventTimestamp })))
}
