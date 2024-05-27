/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.graphql.resource.response.TopicPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.model.Topic
import org.springframework.stereotype.Component

@Component
class TopicPayloadAssembler {

  fun assemble(topic: Topic): TopicPayloadV1 = TopicPayloadMapper.INSTANCE.fromTopic(topic)
}
