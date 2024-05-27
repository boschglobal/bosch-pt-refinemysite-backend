/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.domain.RfvId
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.rest.resource.response.RfvResource
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.model.RfvVersion
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface RfvResourceMapper {

  companion object {
    val INSTANCE: RfvResourceMapper = Mappers.getMapper(RfvResourceMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "id"),
      // Map name explicitly to use the parameter instead of the attribute
      Mapping(source = "name", target = "name"),
      Mapping(
          expression =
              "java(com.bosch.pt.csm.cloud.common.extensions.JavaApiKt.toEpochMilli(rfvVersion.getEventDate()))",
          target = "eventTimestamp"),
      Mapping(expression = "java(rfvVersion.getReason().getKey())", target = "reason"))
  fun fromRfvVersion(
      rfvVersion: RfvVersion,
      project: ProjectId,
      identifier: RfvId,
      language: String,
      name: String
  ): RfvResource
}
