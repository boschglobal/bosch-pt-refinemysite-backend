/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.graphql.resource.response.TaskPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.model.Task
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface TaskPayloadMapper {

  companion object {
    val INSTANCE: TaskPayloadMapper = Mappers.getMapper(TaskPayloadMapper::class.java)
  }

  @Mappings(
      Mapping(source = "task.identifier.value", target = "id"),
      Mapping(source = "task.project", target = "projectId"),
      Mapping(source = "task.craft", target = "craftId"),
      Mapping(source = "task.assignee", target = "assigneeId"),
      Mapping(source = "task.workArea", target = "workAreaId"),
      Mapping(expression = "java(task.getStatus().getShortKey())", target = "status"),
      Mapping(source = "critical", target = "critical"))
  fun fromTask(task: Task, critical: Boolean?): TaskPayloadV1
}
