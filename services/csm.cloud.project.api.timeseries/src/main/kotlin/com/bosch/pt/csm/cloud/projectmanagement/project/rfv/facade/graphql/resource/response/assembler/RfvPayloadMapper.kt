/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.graphql.resource.response.RfvPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.model.Rfv
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface RfvPayloadMapper {

  companion object {
    val INSTANCE: RfvPayloadMapper = Mappers.getMapper(RfvPayloadMapper::class.java)
  }

  @Mappings(
      Mapping(source = "rfv.identifier.value", target = "id"),
      Mapping(source = "name", target = "name"),
      Mapping(expression = "java(rfv.getReason().getShortKey())", target = "reason"))
  fun fromRfv(rfv: Rfv, name: String): RfvPayloadV1
}
