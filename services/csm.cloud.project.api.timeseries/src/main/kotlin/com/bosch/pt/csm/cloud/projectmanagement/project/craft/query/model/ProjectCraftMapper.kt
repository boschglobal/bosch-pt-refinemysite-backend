/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.craft.domain.ProjectCraftId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface ProjectCraftMapper {

  companion object {
    val INSTANCE: ProjectCraftMapper = Mappers.getMapper(ProjectCraftMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "identifier"),
      Mapping(source = "history", target = "history"))
  fun fromProjectCraftVersion(
      projectCraftVersion: ProjectCraftVersion,
      identifier: ProjectCraftId,
      project: ProjectId,
      history: List<ProjectCraftVersion>
  ): ProjectCraft
}
