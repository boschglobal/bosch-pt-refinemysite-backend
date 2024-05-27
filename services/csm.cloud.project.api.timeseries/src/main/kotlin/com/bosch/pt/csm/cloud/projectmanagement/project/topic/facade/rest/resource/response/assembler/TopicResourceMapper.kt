/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.domain.TopicId
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.rest.resource.response.TopicResource
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.model.TopicVersion
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface TopicResourceMapper {

  companion object {
    val INSTANCE: TopicResourceMapper = Mappers.getMapper(TopicResourceMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "id"),
      Mapping(expression = "java(topicVersion.getCriticality().getKey())", target = "criticality"),
      Mapping(
          expression =
              "java(com.bosch.pt.csm.cloud.common.extensions.JavaApiKt.toEpochMilli(topicVersion.getEventDate()))",
          target = "eventTimestamp"))
  fun fromTopicVersion(
      topicVersion: TopicVersion,
      project: ProjectId,
      task: TaskId,
      identifier: TopicId
  ): TopicResource
}
