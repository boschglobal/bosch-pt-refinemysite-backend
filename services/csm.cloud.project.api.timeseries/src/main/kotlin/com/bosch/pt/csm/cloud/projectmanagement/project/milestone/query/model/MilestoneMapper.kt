/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.domain.MilestoneId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface MilestoneMapper {

  companion object {
    val INSTANCE: MilestoneMapper = Mappers.getMapper(MilestoneMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "identifier"),
      Mapping(source = "history", target = "history"))
  fun fromMilestoneVersion(
      milestoneVersion: MilestoneVersion,
      identifier: MilestoneId,
      project: ProjectId,
      history: List<MilestoneVersion>
  ): Milestone
}
