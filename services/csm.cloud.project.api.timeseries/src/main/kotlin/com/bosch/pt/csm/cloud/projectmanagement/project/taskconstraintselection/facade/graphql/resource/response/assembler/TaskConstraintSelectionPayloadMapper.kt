/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.graphql.resource.response.TaskConstraintSelectionPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.model.TaskConstraintSelection
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface TaskConstraintSelectionPayloadMapper {

  companion object {
    val INSTANCE: TaskConstraintSelectionPayloadMapper =
        Mappers.getMapper(TaskConstraintSelectionPayloadMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier.value", target = "id"),
      Mapping(source = "constraints", target = "constraintIds"),
      Mapping(source = "project", target = "projectId"))
  fun fromTaskConstraintSelection(
      constraint: TaskConstraintSelection
  ): TaskConstraintSelectionPayloadV1
}
