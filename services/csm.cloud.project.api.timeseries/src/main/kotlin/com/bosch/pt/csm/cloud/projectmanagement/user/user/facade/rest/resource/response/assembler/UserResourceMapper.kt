/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.rest.resource.response.UserPhoneNumberDto
import com.bosch.pt.csm.cloud.projectmanagement.user.user.facade.rest.resource.response.UserResource
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.model.UserVersion
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface UserResourceMapper {

  companion object {
    val INSTANCE: UserResourceMapper = Mappers.getMapper(UserResourceMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "id"),
      Mapping(source = "phoneNumbers", target = "phoneNumbers"),
      Mapping(
          expression =
              "java(com.bosch.pt.csm.cloud.common.extensions.JavaApiKt.toEpochMilli(user.getEventDate()))",
          target = "eventTimestamp"))
  fun fromUserVersion(
      user: UserVersion,
      identifier: UserId,
      phoneNumbers: List<UserPhoneNumberDto>
  ): UserResource
}
