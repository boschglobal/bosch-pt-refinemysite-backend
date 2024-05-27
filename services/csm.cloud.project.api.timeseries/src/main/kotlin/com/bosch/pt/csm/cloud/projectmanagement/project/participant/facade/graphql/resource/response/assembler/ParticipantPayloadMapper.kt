/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.graphql.resource.response.ParticipantPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.Participant
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface ParticipantPayloadMapper {

  companion object {
    val INSTANCE: ParticipantPayloadMapper = Mappers.getMapper(ParticipantPayloadMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier.value", target = "id"),
      Mapping(source = "company", target = "companyId"),
      Mapping(source = "user", target = "userId"),
      Mapping(expression = "java(participant.getRole().getShortKey())", target = "role"),
      Mapping(expression = "java(participant.getStatus().getShortKey())", target = "status"))
  fun fromParticipant(participant: Participant): ParticipantPayloadV1
}
