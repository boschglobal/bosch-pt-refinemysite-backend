/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.csm.cloud.event.application.security

import org.slf4j.LoggerFactory
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.jwt.Jwt
import reactor.core.publisher.Mono

class CustomReactiveUserAuthenticationConverter(
    private val userDetailsService: ReactiveUserDetailsService,
    private val issuers: List<String>
) : Converter<Jwt, Mono<AbstractAuthenticationToken>> {

  override fun convert(jwt: Jwt): Mono<AbstractAuthenticationToken> {
    if (!issuers.contains(jwt.getClaimAsString(ISSUER_ENTRY))) {
      LOGGER.warn("Invalid issuer {} detected", jwt.getClaimAsString(ISSUER_ENTRY))
      throw OAuth2AuthenticationException(OAuth2Error("invalid issuer"))
    }

    val externalIdClaim =
        if (jwt.hasClaim(BOSCH_ID_ENTRY)) jwt.getClaimAsString(BOSCH_ID_ENTRY)
        else jwt.getClaimAsString(SUBJECT_ENTRY)

    return try {
      userDetailsService
          .findByUsername(externalIdClaim)
          .onErrorMap { exception: Throwable? ->
            OAuth2AuthenticationException(OAuth2Error("unable to load user"), exception)
          }
          .map { user: UserDetails ->
            UsernamePasswordAuthenticationToken(user, "n/a", user.authorities)
          }
    } catch (ex: UsernameNotFoundException) {
      LOGGER.warn("Could not load user", ex)
      throw OAuth2AuthenticationException(OAuth2Error("unable to load user"))
    }
  }

  companion object {
    private const val SUBJECT_ENTRY = "sub"
    private const val BOSCH_ID_ENTRY = "bosch-id"
    private const val ISSUER_ENTRY = "iss"

    private val LOGGER =
        LoggerFactory.getLogger(CustomReactiveUserAuthenticationConverter::class.java)
  }
}
