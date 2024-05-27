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
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface TopicPayloadMapper {

  companion object {
    val INSTANCE: TopicPayloadMapper = Mappers.getMapper(TopicPayloadMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier.value", target = "id"),
      Mapping(expression = "java(topic.getCriticality().getShortKey())", target = "criticality"))
  fun fromTopic(topic: Topic): TopicPayloadV1
}
