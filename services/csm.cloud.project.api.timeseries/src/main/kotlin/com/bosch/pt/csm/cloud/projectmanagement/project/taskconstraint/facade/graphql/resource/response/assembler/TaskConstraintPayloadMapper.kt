/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.graphql.resource.response.TaskConstraintPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.model.TaskConstraint
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface TaskConstraintPayloadMapper {

  companion object {
    val INSTANCE: TaskConstraintPayloadMapper =
        Mappers.getMapper(TaskConstraintPayloadMapper::class.java)
  }

  @Mappings(
      Mapping(source = "constraint.identifier.value", target = "id"),
      Mapping(source = "name", target = "name"),
      Mapping(expression = "java(constraint.getKey().getShortKey())", target = "key"))
  fun fromTaskConstraint(constraint: TaskConstraint, name: String): TaskConstraintPayloadV1
}
