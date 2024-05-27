/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.mail.template

import com.bosch.pt.iot.smartsite.mail.template.MailTemplate

/** Scenario B2 */
data class ParticipantActivatedTemplate(
    val projectUrl: String,
    val recipientName: String,
) : MailTemplate(TEMPLATE_NAME) {

  override val variables = mapOf(PROJECT_URL to projectUrl, RECIPIENT_NAME to recipientName)

  companion object {
    const val TEMPLATE_NAME = "participant-activated"

    const val PROJECT_URL = "project_url"
    const val RECIPIENT_NAME = "recipient_name"
  }
}
