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
import org.springframework.stereotype.Component

@Component
class ParticipantPayloadAssembler {

  fun assemble(participant: Participant): ParticipantPayloadV1 =
      ParticipantPayloadMapper.INSTANCE.fromParticipant(participant)
}
