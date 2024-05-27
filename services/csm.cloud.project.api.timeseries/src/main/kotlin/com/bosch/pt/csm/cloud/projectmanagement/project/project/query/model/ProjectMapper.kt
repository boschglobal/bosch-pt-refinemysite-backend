/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface ProjectMapper {

  companion object {
    val INSTANCE: ProjectMapper = Mappers.getMapper(ProjectMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "identifier"),
      Mapping(source = "history", target = "history"))
  fun fromProjectVersion(
      projectVersion: ProjectVersion,
      identifier: ProjectId,
      history: List<ProjectVersion>
  ): Project
}
