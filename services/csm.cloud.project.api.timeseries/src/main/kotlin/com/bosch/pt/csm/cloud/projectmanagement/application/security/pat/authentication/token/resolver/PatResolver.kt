/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.resolver

import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.PatAuthenticationException
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model.PatErrors
import jakarta.servlet.http.HttpServletRequest
import java.util.regex.Pattern
import java.util.regex.Pattern.CASE_INSENSITIVE
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.security.web.authentication.AuthenticationConverter

class PatResolver : TokenResolver {

  companion object {
    val authorizationPattern: Pattern =
        Pattern.compile("^PAT (?<token>[a-z0-9.]+)$", CASE_INSENSITIVE)
  }

  override fun resolve(request: HttpServletRequest): String? {
    val authorization = request.getHeader(AUTHORIZATION)
    if (authorization?.startsWith("pat", ignoreCase = true) == true) {
      val matcher = authorizationPattern.matcher(authorization)
      if (!matcher.matches()) {
        val error = PatErrors.invalidToken("Personal access token is malformed")
        throw PatAuthenticationException(error)
      }
      return matcher.group("token")
    }
    return null
  }

  override fun setAuthenticationConverter(authenticationConverter: AuthenticationConverter) {
    // do nothing
  }
}
