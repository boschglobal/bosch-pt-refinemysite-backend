/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.handler

import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authorization.InsufficientPatScopeException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders.WWW_AUTHENTICATE
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.oauth2.server.resource.BearerTokenErrorCodes.INSUFFICIENT_SCOPE
import org.springframework.security.web.access.AccessDeniedHandler

/**
 * Translation of
 * [org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler]
 * for PATs.
 */
class PatAccessDeniedHandler : AccessDeniedHandler {

  override fun handle(
      request: HttpServletRequest,
      response: HttpServletResponse,
      accessDeniedException: AccessDeniedException?
  ) {
    response.apply {
      status = FORBIDDEN.value()
      addHeader(
          WWW_AUTHENTICATE,
          computeWWWAuthenticateHeaderValue(
              mutableMapOf<String, String>().apply {
                if (accessDeniedException is InsufficientPatScopeException) {
                  this["error"] = INSUFFICIENT_SCOPE
                  this["error_description"] =
                      accessDeniedException.message
                          ?: "The request requires higher privileges than provided by the access token."
                }
              })
      )
    }
  }

  companion object {

    fun computeWWWAuthenticateHeaderValue(parameters: Map<String, String>): String =
        if (parameters.isNotEmpty()) {
          "PAT " +
              parameters.entries.joinToString(separator = ", ") { (key, value) ->
                "$key=\"$value\""
              }
        } else {
          "PAT is invalid"
        }
  }
}
