/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.rest.resource.response.ParticipantResource
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.Participant
import org.springframework.stereotype.Component

@Component
class ParticipantResourceAssembler {

  fun assemble(
      participant: Participant,
      deleted: Boolean,
      latestOnly: Boolean
  ): List<ParticipantResource> =
      if (latestOnly) {
        listOf(
            ParticipantResourceMapper.INSTANCE.fromParticipant(
                participant.history.last(),
                participant.identifier,
                if (deleted) null else participant.user))
      } else {
        participant.history.map {
          ParticipantResourceMapper.INSTANCE.fromParticipant(
              it, participant.identifier, if (deleted) null else participant.user)
        }
      }
}
