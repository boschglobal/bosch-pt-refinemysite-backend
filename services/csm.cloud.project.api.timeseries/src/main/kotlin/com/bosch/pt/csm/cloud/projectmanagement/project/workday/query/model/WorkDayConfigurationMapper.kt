/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.domain.WorkDayConfigurationId
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface WorkDayConfigurationMapper {

  companion object {
    val INSTANCE: WorkDayConfigurationMapper =
        Mappers.getMapper(WorkDayConfigurationMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "identifier"),
      Mapping(source = "history", target = "history"))
  fun fromWorkDayConfigurationVersion(
      workDayConfigurationVersion: WorkDayConfigurationVersion,
      identifier: WorkDayConfigurationId,
      project: ProjectId,
      history: List<WorkDayConfigurationVersion>
  ): WorkDayConfiguration
}
