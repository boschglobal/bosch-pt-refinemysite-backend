/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.topic.facade.rest.TopicController
import com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.response.TopicListResource
import com.bosch.pt.iot.smartsite.project.topic.shared.model.dto.TopicWithMessageCountDto
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component

@Component
class TopicListResourceFactory(
    private val topicResourceFactoryHelper: TopicResourceFactoryHelper,
    private val customLinkBuilderFactory: CustomLinkBuilderFactory
) {

  fun build(
      topicSlice: Slice<TopicWithMessageCountDto>,
      taskIdentifier: TaskId,
      limit: Int?
  ): TopicListResource {

    var topicLimit =
        when {
          limit == null || limit > TOPIC_DEFAULT_LIMIT -> TOPIC_DEFAULT_LIMIT
          else -> limit
        }

    val topics = topicSlice.content
    return TopicListResource(topicResourceFactoryHelper.build(topics, true)).apply {
      this.addLinks(taskIdentifier, topicLimit, topicSlice.hasNext())
    }
  }

  private fun TopicListResource.addLinks(
      taskIdentifier: TaskId,
      topicLimit: Int,
      hasPrevious: Boolean
  ) {
    addPreviousLink(hasPrevious, taskIdentifier, topicLimit)
  }

  private fun TopicListResource.addPreviousLink(
      hasPrevious: Boolean,
      taskIdentifier: TaskId,
      topicLimit: Int
  ) {
    if (hasPrevious) {
      val nextBefore = topics[topics.size - 1].id
      this.add(
          customLinkBuilderFactory
              .linkTo(TopicController.TOPICS_BY_TASK_ID_ENDPOINT)
              .withParameters(mapOf(TopicController.PATH_VARIABLE_TASK_ID to taskIdentifier))
              .withQueryParameters(mapOf("before" to nextBefore, "limit" to topicLimit))
              .withRel(TopicListResource.LINK_PREVIOUS))
    }
  }

  companion object {
    private const val TOPIC_DEFAULT_LIMIT = 50
  }
}
