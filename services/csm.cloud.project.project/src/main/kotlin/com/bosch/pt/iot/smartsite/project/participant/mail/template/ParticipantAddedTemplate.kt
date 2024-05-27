/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.mail.template

import com.bosch.pt.iot.smartsite.mail.template.MailTemplate

/** Scenario A */
data class ParticipantAddedTemplate(
    val projectUrl: String,
    val projectName: String,
    val originatorName: String,
    val recipientName: String,
) : MailTemplate(TEMPLATE_NAME) {

  override val variables =
      mapOf(
          ORIGINATOR_NAME to originatorName,
          PROJECT_NAME to projectName,
          PROJECT_URL to projectUrl,
          RECIPIENT_NAME to recipientName)

  companion object {
    const val TEMPLATE_NAME = "participant-added"

    const val ORIGINATOR_NAME = "originator_name"
    const val PROJECT_NAME = "project_name"
    const val PROJECT_URL = "project_url"
    const val RECIPIENT_NAME = "recipient_name"
  }
}
