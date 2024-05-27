/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.domain.TaskConstraintId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.rest.resource.response.TaskConstraintResource
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TaskConstraintVersion
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface TaskConstraintResourceMapper {

  companion object {
    val INSTANCE: TaskConstraintResourceMapper =
        Mappers.getMapper(TaskConstraintResourceMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "id"),
      // Map name explicitly to use the parameter instead of the attribute
      Mapping(source = "name", target = "name"),
      Mapping(
          expression =
              "java(com.bosch.pt.csm.cloud.common.extensions.JavaApiKt.toEpochMilli(constraintVersion.getEventDate()))",
          target = "eventTimestamp"),
      Mapping(expression = "java(constraintVersion.getKey().getKey())", target = "key"))
  fun fromTaskConstraintVersion(
      constraintVersion: TaskConstraintVersion,
      project: ProjectId,
      identifier: TaskConstraintId,
      language: String,
      name: String
  ): TaskConstraintResource
}
