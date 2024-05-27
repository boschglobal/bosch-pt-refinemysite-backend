/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.domain.TaskConstraintSelectionId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.facade.rest.resource.response.TaskConstraintSelectionResource
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.model.TaskConstraintSelectionVersion
import java.time.LocalDateTime
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface TaskConstraintSelectionResourceMapper {

  companion object {
    val INSTANCE: TaskConstraintSelectionResourceMapper =
        Mappers.getMapper(TaskConstraintSelectionResourceMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "id"),
      Mapping(source = "key", target = "key"),
      Mapping(
          expression =
              "java(com.bosch.pt.csm.cloud.common.extensions.JavaApiKt.toEpochMilli(eventDate))",
          target = "eventTimestamp"))
  fun fromTaskConstraintVersion(
      constraintVersion: TaskConstraintSelectionVersion,
      project: ProjectId,
      task: TaskId,
      identifier: TaskConstraintSelectionId,
      key: String,
      eventDate: LocalDateTime
  ): TaskConstraintSelectionResource
}
