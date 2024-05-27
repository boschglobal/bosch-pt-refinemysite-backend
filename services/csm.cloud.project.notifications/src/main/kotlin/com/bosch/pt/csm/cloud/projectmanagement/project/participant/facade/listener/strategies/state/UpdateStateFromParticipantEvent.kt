/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.listener.strategies.state

import com.bosch.pt.csm.cloud.common.businesstransaction.boundary.EventRecord
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.UpdateStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getCompanyIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.CANCELLED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.INVITED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.VALIDATION
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.boundary.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.model.Participant
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.model.ParticipantRoleEnum
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class UpdateStateFromParticipantEvent(private val participantService: ParticipantService) :
    AbstractStateStrategy<ParticipantEventG3Avro>(), UpdateStateStrategy {

  override fun handles(record: EventRecord) =
      record.value.run {
        this is ParticipantEventG3Avro &&
            this.name != CANCELLED &&
            when (this.aggregate.status) {
              VALIDATION,
              INVITED -> false
              else -> true
            }
      }

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: ParticipantEventG3Avro): Unit =
      event.aggregate.run {
        participantService.save(
            Participant(
                identifier = getIdentifier(),
                role = ParticipantRoleEnum.valueOf(role.name),
                projectIdentifier = getProjectIdentifier(),
                companyIdentifier = getCompanyIdentifier()!!,
                userIdentifier = getUserIdentifier()!!,
                active = event.name != ParticipantEventEnumAvro.DEACTIVATED))
      }
}
