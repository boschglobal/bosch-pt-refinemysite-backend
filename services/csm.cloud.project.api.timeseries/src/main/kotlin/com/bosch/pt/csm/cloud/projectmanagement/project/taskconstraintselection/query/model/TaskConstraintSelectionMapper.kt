/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.domain.TaskConstraintSelectionId
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface TaskConstraintSelectionMapper {

  companion object {
    val INSTANCE: TaskConstraintSelectionMapper =
        Mappers.getMapper(TaskConstraintSelectionMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "identifier"),
      Mapping(source = "history", target = "history"))
  fun fromTaskConstraintSelectionVersion(
      taskConstraintSelectionVersion: TaskConstraintSelectionVersion,
      identifier: TaskConstraintSelectionId,
      project: ProjectId,
      task: TaskId,
      history: List<TaskConstraintSelectionVersion>
  ): TaskConstraintSelection
}
