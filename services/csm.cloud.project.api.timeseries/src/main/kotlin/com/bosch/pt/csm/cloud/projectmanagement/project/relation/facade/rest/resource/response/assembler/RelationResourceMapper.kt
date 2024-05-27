/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.relation.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.domain.RelationId
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.facade.rest.resource.response.RelationResource
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.model.RelationVersion
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface RelationResourceMapper {

  companion object {
    val INSTANCE: RelationResourceMapper = Mappers.getMapper(RelationResourceMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "id"),
      Mapping(expression = "java(relationVersion.getType().getKey())", target = "type"),
      Mapping(
          expression =
              "java(com.bosch.pt.csm.cloud.common.extensions.JavaApiKt.toEpochMilli(relationVersion.getEventDate()))",
          target = "eventTimestamp"))
  fun fromRelationVersion(
      relationVersion: RelationVersion,
      project: ProjectId,
      identifier: RelationId
  ): RelationResource
}
