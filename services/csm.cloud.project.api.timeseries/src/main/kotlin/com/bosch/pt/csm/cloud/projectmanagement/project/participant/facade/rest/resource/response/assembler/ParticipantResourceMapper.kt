/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.participant.domain.ParticipantId
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.rest.resource.response.ParticipantResource
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.ParticipantVersion
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface ParticipantResourceMapper {

  companion object {
    val INSTANCE: ParticipantResourceMapper =
        Mappers.getMapper(ParticipantResourceMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "id"),
      Mapping(source = "userId", target = "user"),
      Mapping(expression = "java(participant.getRole().getKey())", target = "role"),
      Mapping(expression = "java(participant.getStatus().getKey())", target = "status"),
      Mapping(
          expression =
              "java(com.bosch.pt.csm.cloud.common.extensions.JavaApiKt.toEpochMilli(participant.getEventDate()))",
          target = "eventTimestamp"))
  fun fromParticipant(
      participant: ParticipantVersion,
      identifier: ParticipantId,
      userId: UserId?
  ): ParticipantResource
}
