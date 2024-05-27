/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.domain.WorkDayConfigurationId
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.rest.resource.response.WorkDayConfigurationResource
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.model.WorkDayConfigurationVersion
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface WorkDayConfigurationResourceMapper {

  companion object {
    val INSTANCE: WorkDayConfigurationResourceMapper =
        Mappers.getMapper(WorkDayConfigurationResourceMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "id"),
      Mapping(
          expression = "java(workDayConfigurationVersion.getStartOfWeek().getKey())",
          target = "startOfWeek"),
      Mapping(source = "workingDays", target = "workingDays"),
      Mapping(
          expression =
              "java(com.bosch.pt.csm.cloud.common.extensions.JavaApiKt.toEpochMilli(" +
                  "workDayConfigurationVersion.getEventDate()))",
          target = "eventTimestamp"))
  fun fromWorkDayConfigurationVersion(
      workDayConfigurationVersion: WorkDayConfigurationVersion,
      project: ProjectId,
      identifier: WorkDayConfigurationId,
      workingDays: List<String>
  ): WorkDayConfigurationResource
}
