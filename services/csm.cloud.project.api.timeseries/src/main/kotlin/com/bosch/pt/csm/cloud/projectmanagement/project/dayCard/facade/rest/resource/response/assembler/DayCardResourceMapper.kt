/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.domain.DayCardId
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.rest.resource.response.DayCardResource
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCardVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import java.time.LocalDate
import java.time.LocalDateTime
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface DayCardResourceMapper {

  companion object {
    val INSTANCE: DayCardResourceMapper = Mappers.getMapper(DayCardResourceMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "id"),
      // Set the mappings explicitly to overwrite the values from
      // the dayCardVersion attribute
      Mapping(expression = "java(dayCardVersion.getStatus().getKey())", target = "status"),
      Mapping(source = "date", target = "date"),
      Mapping(
          expression =
              "java(com.bosch.pt.csm.cloud.common.extensions.JavaApiKt.toEpochMilli(eventDate))",
          target = "eventTimestamp"),
      Mapping(source = "reason", target = "reason"))
  fun fromDayCardVersion(
      dayCardVersion: DayCardVersion,
      project: ProjectId,
      task: TaskId,
      identifier: DayCardId,
      date: LocalDate,
      reason: String?,
      eventDate: LocalDateTime
  ): DayCardResource
}
