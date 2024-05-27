/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.graphql.resource.response.ProjectPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.model.Project
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface ProjectPayloadMapper {

  companion object {
    val INSTANCE: ProjectPayloadMapper = Mappers.getMapper(ProjectPayloadMapper::class.java)
  }

  @Mappings(
      Mapping(source = "projectVersion.identifier.value", target = "id"),
      Mapping(source = "category", target = "category"))
  fun fromProject(projectVersion: Project, category: String?): ProjectPayloadV1
}
