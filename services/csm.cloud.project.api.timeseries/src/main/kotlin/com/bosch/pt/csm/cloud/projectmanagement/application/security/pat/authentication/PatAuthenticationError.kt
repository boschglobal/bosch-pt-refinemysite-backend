/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication

import java.io.Serializable

/** Translation of [org.springframework.security.oauth2.core.OAuth2Error] for PATs. */
open class PatAuthenticationError(
    val errorCode: String,
    val description: String? = null,
    val uri: String? = null
) : Serializable {

  companion object {
    private const val serialVersionUID = -8202852851128758542L
  }
}
