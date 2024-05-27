/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model

import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model.PatErrorCodes.INVALID_TOKEN
import org.springframework.http.HttpStatus.UNAUTHORIZED

/**
 * Translation of [org.springframework.security.oauth2.server.resource.BearerTokenErrors] for PATs.
 */
object PatErrors {

  private const val DEFAULT_URI = "https://tools.ietf.org/html/rfc6750#section-3.1"

  private val DEFAULT_INVALID_TOKEN =
      PatTokenError(UNAUTHORIZED, INVALID_TOKEN, "Invalid token", DEFAULT_URI)

  fun invalidToken(message: String) =
      try {
        PatTokenError(UNAUTHORIZED, INVALID_TOKEN, message, DEFAULT_URI)
      } catch (@Suppress("SwallowedException") e: IllegalArgumentException) {
        DEFAULT_INVALID_TOKEN
      }
}
