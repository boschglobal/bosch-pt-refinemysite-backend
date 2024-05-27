/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.graphql.resource.response.TaskSchedulePayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.model.TaskSchedule
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface TaskSchedulePayloadMapper {

  companion object {
    val INSTANCE: TaskSchedulePayloadMapper =
        Mappers.getMapper(TaskSchedulePayloadMapper::class.java)
  }

  @Mappings(Mapping(source = "identifier.value", target = "id"))
  fun fromTaskSchedule(taskSchedule: TaskSchedule): TaskSchedulePayloadV1
}
