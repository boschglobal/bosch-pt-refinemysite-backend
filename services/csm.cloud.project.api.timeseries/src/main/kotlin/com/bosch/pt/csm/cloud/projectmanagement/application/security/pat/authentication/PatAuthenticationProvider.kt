/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication

import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.PatAuthenticationToken
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.details.PatUserDetailsAuthenticationToken
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model.PatTokenParser.parse
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model.PatToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication

class PatAuthenticationProvider(
    private val patAuthenticationTokenConverter:
        Converter<PatToken, out AbstractAuthenticationToken>
) : AuthenticationProvider {

  companion object {
    val logger: Logger = LoggerFactory.getLogger(PatAuthenticationProvider::class.java)
  }

  override fun authenticate(authentication: Authentication?): Authentication {
    val authenticationToken =
        when (authentication) {
          is PatAuthenticationToken -> authentication
          null -> throw InvalidPatException("Invalid pat")
          else -> throw InvalidPatException("Invalid pat type")
        }

    val pat = parse(authenticationToken.token) ?: throw InvalidPatException("Invalid pat")
    val token: AbstractAuthenticationToken =
        checkNotNull(patAuthenticationTokenConverter.convert(pat))
    logger.debug("Authenticated token")
    return token
  }

  override fun supports(authentication: Class<*>): Boolean =
      PatUserDetailsAuthenticationToken::class.java.isAssignableFrom(authentication)
}
