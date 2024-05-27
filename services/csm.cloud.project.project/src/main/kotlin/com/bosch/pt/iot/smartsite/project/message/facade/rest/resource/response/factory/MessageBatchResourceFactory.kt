/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.resource.factory.PageLinks
import com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.response.MessageBatchResource
import com.bosch.pt.iot.smartsite.project.message.shared.model.dto.MessageDto
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component

@Component
class MessageBatchResourceFactory(
    private val messageResourceFactoryHelper: MessageResourceFactoryHelper
) {

  @PageLinks
  fun build(messages: Slice<MessageDto>): MessageBatchResource =
      MessageBatchResource(
          messageResourceFactoryHelper.build(messages.content, false),
          messages.number,
          messages.size)
}
