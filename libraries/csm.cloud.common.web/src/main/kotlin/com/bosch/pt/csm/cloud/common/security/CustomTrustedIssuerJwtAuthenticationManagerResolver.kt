/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.security

import java.util.concurrent.ConcurrentHashMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationManagerResolver
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.JwtDecoderFactory
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider

/**
 * Determines an authentication manager based on the issuer found in the JWT or null if the issuer
 * is not trusted. The authentication manager will utilize the instance of
 * [AbstractCustomUserAuthenticationConverter] configured to determine the authentication
 */
class CustomTrustedIssuerJwtAuthenticationManagerResolver(
    private val trustedJwtIssuersConfig: CustomTrustedJwtIssuersProperties,
    private val converter: AbstractCustomUserAuthenticationConverter,
    private val jwtDecoderFactory: JwtDecoderFactory<String>
) : AuthenticationManagerResolver<String> {

  private val authenticationManagers: MutableMap<String, AuthenticationManager> =
      ConcurrentHashMap()

  override fun resolve(issuer: String): AuthenticationManager? {
    if (trustedJwtIssuersConfig.issuerUris.contains(issuer)) {
      val authenticationManager =
          authenticationManagers.computeIfAbsent(issuer) { _: String ->
            logger.debug("Constructing AuthenticationManager")
            val jwtDecoder = jwtDecoderFactory.createDecoder(issuer)
            AuthenticationManager { authentication: Authentication ->
              val jwtAuthenticationProvider = JwtAuthenticationProvider(jwtDecoder)
              jwtAuthenticationProvider.setJwtAuthenticationConverter(this.converter)
              jwtAuthenticationProvider.authenticate(authentication)
            }
          }
      logger.debug("Resolved AuthenticationManager for issuer $issuer")
      return authenticationManager
    } else {
      logger.debug(
          "Did not resolve AuthenticationManager since issuer $issuer is not trusted. Auth should fail..")
      return null
    }
  }

  companion object {
    private val logger: Logger =
        LoggerFactory.getLogger(CustomTrustedIssuerJwtAuthenticationManagerResolver::class.java)
  }
}
