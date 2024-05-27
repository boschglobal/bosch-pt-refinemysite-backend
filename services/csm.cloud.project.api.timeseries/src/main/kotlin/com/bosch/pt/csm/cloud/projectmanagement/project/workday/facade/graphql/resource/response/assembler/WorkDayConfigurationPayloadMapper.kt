/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.graphql.resource.response.HolidayPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.graphql.resource.response.WorkDayConfigurationPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.model.WorkDayConfiguration
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface WorkDayConfigurationPayloadMapper {

  companion object {
    val INSTANCE: WorkDayConfigurationPayloadMapper =
        Mappers.getMapper(WorkDayConfigurationPayloadMapper::class.java)
  }

  @Mappings(Mapping(source = "workDayConfiguration.identifier.value", target = "id"))
  fun fromWorkDayConfiguration(
      workDayConfiguration: WorkDayConfiguration,
      workingDays: List<String>,
      holidays: List<HolidayPayloadV1>
  ): WorkDayConfigurationPayloadV1
}
