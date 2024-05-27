/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.iot.smartsite.project.message.facade.rest.MessageController.Companion.MESSAGES_BY_TOPIC_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.message.facade.rest.MessageController.Companion.PATH_VARIABLE_TOPIC_ID
import com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.response.MessageListResource
import com.bosch.pt.iot.smartsite.project.message.shared.model.dto.MessageDto
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component

@Component
class MessageListResourceFactory(
    private val messageResourceFactoryHelper: MessageResourceFactoryHelper,
    private val linkFactory: CustomLinkBuilderFactory
) {

  fun build(messages: Slice<MessageDto>, topicId: TopicId, limit: Int?): MessageListResource {
    val messageLimit =
        when {
          limit == null || limit > MESSAGE_DEFAULT_LIMIT -> MESSAGE_DEFAULT_LIMIT
          else -> limit
        }
    val messageList = messages.content
    return MessageListResource(messageResourceFactoryHelper.build(messageList, true)).apply {
      addPreviousLink(messages.hasNext(), topicId, messageLimit)
    }
  }

  private fun MessageListResource.addPreviousLink(
      hasPrevious: Boolean,
      topicIdentifier: TopicId,
      messageLimit: Int
  ) {
    addIf(hasPrevious) {
      linkFactory
          .linkTo(MESSAGES_BY_TOPIC_ID_ENDPOINT)
          .withParameters(mapOf(PATH_VARIABLE_TOPIC_ID to topicIdentifier))
          .withQueryParameters(
              mapOf("before" to messages[messages.size - 1].id, "limit" to messageLimit))
          .withRel(MessageListResource.LINK_PREVIOUS)
    }
  }

  companion object {
    private const val MESSAGE_DEFAULT_LIMIT = 50
  }
}
