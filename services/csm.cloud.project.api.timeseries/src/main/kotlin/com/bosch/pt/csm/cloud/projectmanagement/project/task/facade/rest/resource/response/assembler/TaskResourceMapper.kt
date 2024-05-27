/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.facade.rest.resource.response.TaskResource
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.model.TaskVersion
import java.time.LocalDate
import java.time.LocalDateTime
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface TaskResourceMapper {

  companion object {
    val INSTANCE: TaskResourceMapper = Mappers.getMapper(TaskResourceMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "id"),
      Mapping(expression = "java(taskVersion.getStatus().getKey())", target = "status"),
      Mapping(
          expression =
              "java(com.bosch.pt.csm.cloud.common.extensions.JavaApiKt.toEpochMilli(eventDate))",
          target = "eventTimestamp"))
  fun fromTaskVersion(
      taskVersion: TaskVersion,
      project: ProjectId,
      identifier: TaskId,
      start: LocalDate?,
      end: LocalDate?,
      eventDate: LocalDateTime
  ): TaskResource
}
