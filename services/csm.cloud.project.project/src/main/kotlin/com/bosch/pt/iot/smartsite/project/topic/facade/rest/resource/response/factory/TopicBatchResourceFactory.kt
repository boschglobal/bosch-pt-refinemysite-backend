/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.resource.factory.PageLinks
import com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.response.TopicBatchResource
import com.bosch.pt.iot.smartsite.project.topic.shared.model.dto.TopicWithMessageCountDto
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component

@Component
open class TopicBatchResourceFactory(
    private val topicResourceFactoryHelper: TopicResourceFactoryHelper
) {

  @PageLinks
  open fun build(topicSlice: Slice<TopicWithMessageCountDto>): TopicBatchResource =
      TopicBatchResource(
          topicResourceFactoryHelper.build(topicSlice.content, false),
          topicSlice.number,
          topicSlice.size)
}
