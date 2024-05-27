/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.test

import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.NotificationSummaryDto
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.PlaceholderValueDto
import com.bosch.pt.csm.cloud.projectmanagement.participant.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro

object NotificationSummaryGenerator {

  fun buildSummary(
      template: String,
      actorParticipant: ParticipantAggregateG3Avro,
      actorUser: UserAggregateAvro
  ) =
      NotificationSummaryDto(
          template = template,
          values =
              mapOf(
                  "originator" to
                      PlaceholderValueDto(
                          type = "PARTICIPANT",
                          id = actorParticipant.getIdentifier(),
                          text = "${actorUser.getFirstName()} ${actorUser.getLastName()}")))
}
