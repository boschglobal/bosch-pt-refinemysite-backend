/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.resolver

import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.PatAuthenticationException
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model.PatErrors.invalidToken
import jakarta.servlet.http.HttpServletRequest
import java.util.regex.Pattern
import java.util.regex.Pattern.CASE_INSENSITIVE
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationConverter

class BasicPatResolver : TokenResolver {

  private lateinit var authenticationConverter: AuthenticationConverter

  companion object {
    private val authorizationPattern: Pattern =
        Pattern.compile("^(?<token>[a-z0-9.]+)$", CASE_INSENSITIVE)
  }

  override fun resolve(request: HttpServletRequest): String? {
    // Decode the base 64 encoded token in the format through the converter
    // <empty-string-as-username>:<pat-as-credential>
    val token: Authentication?
    try {
      token = authenticationConverter.convert(request)
    } catch (e: BadCredentialsException) {
      throw PatAuthenticationException(invalidToken("Invalid basic authentication token"), e)
    }

    // Check pat format
    if (token?.credentials != null) {
      val matcher = authorizationPattern.matcher(token.credentials.toString())
      if (!matcher.matches()) {
        throw PatAuthenticationException(invalidToken("Personal access token is malformed"))
      }
      return matcher.group("token")
    }
    return null
  }

  override fun setAuthenticationConverter(authenticationConverter: AuthenticationConverter) {
    this.authenticationConverter = authenticationConverter
  }
}
