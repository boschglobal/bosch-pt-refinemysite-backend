/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.user.query.model

import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface UserProjectionMapper {

  companion object {
    val INSTANCE: UserProjectionMapper = Mappers.getMapper(UserProjectionMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "identifier"),
      Mapping(source = "history", target = "history"),
      Mapping(target = "authorities", ignore = true))
  fun fromUserVersion(
      userVersion: UserVersion,
      identifier: UserId,
      history: List<UserVersion>
  ): UserProjection
}
