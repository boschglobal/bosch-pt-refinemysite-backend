/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication

import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model.PatToken
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatTypeEnum
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationManagerResolver
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver

/** Translation of [JwtIssuerAuthenticationManagerResolver] for PATs. */
class PatAuthenticationManagerResolver(
    converters: Map<PatTypeEnum, Converter<PatToken, out AbstractAuthenticationToken?>>
) : AuthenticationManagerResolver<HttpServletRequest> {

  private val authenticationManagers: Map<PatTypeEnum, AuthenticationManager> =
      converters
          .map { (patType, converter) ->
            patType to
                object : AuthenticationManager {
                  private val authProvider = PatAuthenticationProvider(converter)
                  override fun authenticate(authentication: Authentication?): Authentication =
                      authProvider.authenticate(authentication)
                }
          }
          .associate { it.first to it.second }

  private val authenticationManager = PatAuthenticationManager { type ->
    requireNotNull(authenticationManagers[type])
  }

  override fun resolve(context: HttpServletRequest?): AuthenticationManager = authenticationManager
}
