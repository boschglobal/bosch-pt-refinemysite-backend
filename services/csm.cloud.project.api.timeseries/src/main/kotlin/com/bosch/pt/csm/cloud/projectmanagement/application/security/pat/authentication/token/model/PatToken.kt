/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model

import java.util.UUID

data class PatToken(
    // Version of the pat, e.g. RMSPAT1
    val type: String,
    // Identifier of the pat
    val patId: UUID,
    // Raw value of the user-secret
    val secret: String,
) {

  private val shortenedPatId = patId.toString().replace("-", "")

  override fun toString(): String = "$type.$shortenedPatId.$secret"

  companion object {
    const val ID_SEPARATOR = "-"
    const val SEPARATOR = "."
  }
}
