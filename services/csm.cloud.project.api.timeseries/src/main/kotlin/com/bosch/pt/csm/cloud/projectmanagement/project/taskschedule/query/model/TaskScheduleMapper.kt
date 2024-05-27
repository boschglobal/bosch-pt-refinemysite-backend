/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.domain.TaskScheduleId
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface TaskScheduleMapper {

  companion object {
    val INSTANCE: TaskScheduleMapper = Mappers.getMapper(TaskScheduleMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "identifier"),
      Mapping(source = "history", target = "history"))
  fun fromTaskScheduleVersion(
      taskScheduleVersion: TaskScheduleVersion,
      identifier: TaskScheduleId,
      project: ProjectId,
      task: TaskId,
      history: List<TaskScheduleVersion>
  ): TaskSchedule
}
