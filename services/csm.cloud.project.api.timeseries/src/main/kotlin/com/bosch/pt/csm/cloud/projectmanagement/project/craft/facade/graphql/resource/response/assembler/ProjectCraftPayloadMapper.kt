/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.graphql.resource.response.ProjectCraftPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.model.ProjectCraft
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface ProjectCraftPayloadMapper {

  companion object {
    val INSTANCE: ProjectCraftPayloadMapper =
        Mappers.getMapper(ProjectCraftPayloadMapper::class.java)
  }

  @Mappings(Mapping(source = "identifier.value", target = "id"))
  fun fromProjectCraft(projectCraft: ProjectCraft): ProjectCraftPayloadV1
}
