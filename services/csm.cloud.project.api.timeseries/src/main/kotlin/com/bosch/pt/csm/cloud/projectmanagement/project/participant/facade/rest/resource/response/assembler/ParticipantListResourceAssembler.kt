/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.rest.resource.response.ParticipantListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.Participant
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import org.springframework.stereotype.Component

@Component
class ParticipantListResourceAssembler(
    private val participantResourceAssembler: ParticipantResourceAssembler
) {

  fun assemble(
      participants: List<Participant>,
      idsOfExistingUses: List<UserId>,
      latestOnly: Boolean
  ): ParticipantListResource =
      ParticipantListResource(
          participants
              .flatMap {
                participantResourceAssembler.assemble(
                    it, !idsOfExistingUses.contains(it.user), latestOnly)
              }
              .sortedWith(compareBy({ it.id.value }, { it.version }, { it.eventTimestamp })))
}
