/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.rest.resource.response.ProjectResource
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.model.ProjectVersion
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface ProjectResourceMapper {

  companion object {
    val INSTANCE: ProjectResourceMapper = Mappers.getMapper(ProjectResourceMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "id"),
      Mapping(source = "category", target = "category"),
      Mapping(
          expression =
              "java(com.bosch.pt.csm.cloud.common.extensions.JavaApiKt.toEpochMilli(project.getEventDate()))",
          target = "eventTimestamp"))
  fun fromProject(
      project: ProjectVersion,
      identifier: ProjectId,
      category: String?
  ): ProjectResource
}
