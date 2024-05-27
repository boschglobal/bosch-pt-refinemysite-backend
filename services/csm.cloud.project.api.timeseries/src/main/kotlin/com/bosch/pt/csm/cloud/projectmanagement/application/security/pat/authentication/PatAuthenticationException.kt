/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication

import org.springframework.security.core.AuthenticationException

/**
 * Translation of [org.springframework.security.oauth2.core.OAuth2AuthenticationException] for PATs.
 */
open class PatAuthenticationException(
    val error: PatAuthenticationError,
    message: String,
    cause: Throwable? = null
) : AuthenticationException(message, cause) {

  constructor(error: PatAuthenticationError) : this(error, error.description ?: "")

  constructor(
      error: PatAuthenticationError,
      cause: Throwable?
  ) : this(error, error.description ?: "", cause)

  constructor(error: PatAuthenticationError, message: String) : this(error, message, null)
}
