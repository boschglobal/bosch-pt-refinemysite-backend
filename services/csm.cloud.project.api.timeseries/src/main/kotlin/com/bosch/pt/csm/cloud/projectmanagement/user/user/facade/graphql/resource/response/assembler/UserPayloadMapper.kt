/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.graphql.resource.response.UserPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.graphql.resource.response.UserPhoneNumberPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.model.UserProjection
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface UserPayloadMapper {

  companion object {
    val INSTANCE: UserPayloadMapper = Mappers.getMapper(UserPayloadMapper::class.java)
  }

  @Mappings(
      Mapping(source = "userVersion.identifier.value", target = "id"),
      Mapping(source = "phoneNumbers", target = "phoneNumbers"))
  fun fromUserProjection(
      userVersion: UserProjection,
      phoneNumbers: List<UserPhoneNumberPayloadV1>
  ): UserPayloadV1
}
