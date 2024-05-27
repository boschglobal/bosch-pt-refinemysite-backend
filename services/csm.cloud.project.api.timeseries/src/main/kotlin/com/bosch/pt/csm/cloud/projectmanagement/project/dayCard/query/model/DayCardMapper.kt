/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.domain.DayCardId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface DayCardMapper {

  companion object {
    val INSTANCE: DayCardMapper = Mappers.getMapper(DayCardMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "identifier"),
      Mapping(source = "history", target = "history"))
  fun fromDayCardVersion(
      dayCardVersion: DayCardVersion,
      identifier: DayCardId,
      project: ProjectId,
      task: TaskId,
      history: List<DayCardVersion>
  ): DayCard
}
