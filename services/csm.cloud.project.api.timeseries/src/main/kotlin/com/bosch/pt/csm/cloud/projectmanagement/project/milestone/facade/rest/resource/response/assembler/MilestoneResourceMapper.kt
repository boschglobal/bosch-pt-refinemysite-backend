/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.domain.MilestoneId
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.rest.resource.response.MilestoneResource
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.model.MilestoneVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface MilestoneResourceMapper {

  companion object {
    val INSTANCE: MilestoneResourceMapper = Mappers.getMapper(MilestoneResourceMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "id"),
      Mapping(source = "milestoneVersion.header", target = "global"),
      Mapping(expression = "java(milestoneVersion.getType().getKey())", target = "type"),
      Mapping(
          expression =
              "java(com.bosch.pt.csm.cloud.common.extensions.JavaApiKt.toEpochMilli(milestoneVersion.getEventDate()))",
          target = "eventTimestamp"))
  fun fromMilestoneVersion(
      milestoneVersion: MilestoneVersion,
      project: ProjectId,
      identifier: MilestoneId
  ): MilestoneResource
}
