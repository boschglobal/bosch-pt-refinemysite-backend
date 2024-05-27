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
import com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.NotificationService
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.AbstractStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener.strategies.state.CleanUpStateStrategy
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getUserIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro.DEACTIVATED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class CleanupStateFromParticipantDeactivatedEvent(
    private val notificationService: NotificationService
) : AbstractStateStrategy<ParticipantEventG3Avro>(), CleanUpStateStrategy {

  override fun handles(record: EventRecord) =
      record.value is ParticipantEventG3Avro &&
          (record.value as ParticipantEventG3Avro).name == DEACTIVATED

  @Trace
  override fun updateState(messageKey: EventMessageKey, event: ParticipantEventG3Avro) =
      event.aggregate.run {
        notificationService.deleteNotifications(getUserIdentifier()!!, getProjectIdentifier())
      }
}
