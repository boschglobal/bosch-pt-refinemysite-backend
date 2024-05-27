/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.mail.template

import com.bosch.pt.iot.smartsite.mail.template.MailTemplate

/** Scenario B1 */
data class ParticipantInvitedTemplate(
    val projectName: String,
    val originatorName: String,
    val registerUrl: String,
    val expirationDate: Map<String, String>
) : MailTemplate(TEMPLATE_NAME) {

  override val variables =
      mapOf(
          ORIGINATOR_NAME to originatorName,
          PROJECT_NAME to projectName,
          REGISTER_URL to registerUrl,
          EXPIRATION_DATE to expirationDate)

  companion object {
    const val TEMPLATE_NAME = "participant-invited"

    const val ORIGINATOR_NAME = "originator_name"
    const val PROJECT_NAME = "project_name"

    const val REGISTER_URL = "register_url"
    const val EXPIRATION_DATE = "expiration_date"
  }
}
