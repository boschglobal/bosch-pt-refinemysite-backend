/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.craft.domain.ProjectCraftId
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.rest.resource.response.ProjectCraftResource
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.model.ProjectCraftVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface ProjectCraftResourceMapper {

  companion object {
    val INSTANCE: ProjectCraftResourceMapper =
        Mappers.getMapper(ProjectCraftResourceMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "id"),
      Mapping(
          expression =
              "java(com.bosch.pt.csm.cloud.common.extensions.JavaApiKt.toEpochMilli(" +
                  "projectCraftVersion.getEventDate()))",
          target = "eventTimestamp"))
  fun fromProjectCraftVersion(
      projectCraftVersion: ProjectCraftVersion,
      project: ProjectId,
      identifier: ProjectCraftId
  ): ProjectCraftResource
}
