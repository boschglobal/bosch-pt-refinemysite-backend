/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.response.TopicResource
import com.bosch.pt.iot.smartsite.project.topic.shared.model.dto.TopicWithMessageCountDto
import org.springframework.stereotype.Component

@Component
open class TopicResourceFactory(
    private val topicResourceFactoryHelper: TopicResourceFactoryHelper
) {
  open fun build(topic: TopicWithMessageCountDto): TopicResource =
      topicResourceFactoryHelper.build(listOf(topic), true).first()
}
