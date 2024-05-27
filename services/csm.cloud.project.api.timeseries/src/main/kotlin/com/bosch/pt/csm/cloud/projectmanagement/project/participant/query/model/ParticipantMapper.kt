/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.participant.domain.ParticipantId
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface ParticipantMapper {

  companion object {
    val INSTANCE: ParticipantMapper = Mappers.getMapper(ParticipantMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "identifier"),
      Mapping(source = "history", target = "history"))
  fun fromParticipantVersion(
      participantVersion: ParticipantVersion,
      identifier: ParticipantId,
      history: List<ParticipantVersion>
  ): Participant
}
