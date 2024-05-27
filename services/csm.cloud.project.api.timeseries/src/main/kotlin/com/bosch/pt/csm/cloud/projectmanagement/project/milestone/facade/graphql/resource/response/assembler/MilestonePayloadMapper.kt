/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.graphql.resource.response.MilestonePayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.model.Milestone
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface MilestonePayloadMapper {

  companion object {
    val INSTANCE: MilestonePayloadMapper = Mappers.getMapper(MilestonePayloadMapper::class.java)
  }

  @Mappings(
      Mapping(source = "milestone.identifier.value", target = "id"),
      Mapping(source = "milestone.project", target = "projectId"),
      Mapping(source = "milestone.craft", target = "craftId"),
      Mapping(source = "milestone.workArea", target = "workAreaId"),
      Mapping(source = "milestone.header", target = "global"),
      Mapping(expression = "java(milestone.getType().getShortKey())", target = "type"),
      Mapping(source = "critical", target = "critical"))
  fun fromMilestone(milestone: Milestone, critical: Boolean?): MilestonePayloadV1
}
