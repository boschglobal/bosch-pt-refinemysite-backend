/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.security

import org.slf4j.LoggerFactory.getLogger
import org.springframework.core.NestedRuntimeException
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Converts [Jwt] to [AbstractAuthenticationToken] by picking either 'bosch-id' field or 'sub' field
 * from the JWT depending on the token at hand. Also ensures that implementations of [JwtVerificationListener]
 * are run allowing to exchange [UserDetails] returned according to the transformation(s).
 *
 * Requires to implement [getUserDetails] as required. A default implementation is
 * [DefaultCustomUserAuthenticationConverter] that will be autowired by
 * [CustomWebSecurityAutoConfiguration] if no specific implementation is provided.
 */
abstract class AbstractCustomUserAuthenticationConverter(
    private val jwtVerificationListeners: List<JwtVerificationListener>,
    private val trustedJwtIssuersProperties: CustomTrustedJwtIssuersProperties
) : Converter<Jwt, AbstractAuthenticationToken> {

  override fun convert(jwt: Jwt): AbstractAuthenticationToken = extractAuthentication(jwt.claims)

  private fun extractAuthentication(map: Map<String, *>): UsernamePasswordAuthenticationToken {

    // Check if token contains a trusted issuer - to be sure
    // (may have already been done by authentication manager resolver)
    val issuerValue = map[ISSUER_ENTRY]
    if (issuerValue == null ||
        trustedJwtIssuersProperties.issuerUris.find {
          issuerValue.toString().equals(it, ignoreCase = true)
        } == null) {
      logger.warn("Invalid issuer $issuerValue detected")
      throw OAuth2AuthenticationException(OAuth2Error("Untrusted issuer $issuerValue"))
    }

    return if (map.containsKey(BOSCH_ID_ENTRY)) {
      usernamePasswordAuthenticationToken(map, BOSCH_ID_ENTRY)
    } else if (map.containsKey(SUBJECT_ENTRY)) {
      usernamePasswordAuthenticationToken(map, SUBJECT_ENTRY)
    } else {
      throw OAuth2AuthenticationException(OAuth2Error("missing sub"))
    }
  }

  private fun usernamePasswordAuthenticationToken(
      map: Map<String, *>,
      key: String
  ): UsernamePasswordAuthenticationToken =
      try {
        // normal flow with interactive user authentication
        var principal = getUserDetails(map[key] as String?, map)
        jwtVerificationListeners.forEach {
          principal = it.onJwtVerifiedEvent(map, principal)
        }
        UsernamePasswordAuthenticationToken(principal, NOT_AVAILABLE, principal.authorities)
      } catch (dex: NestedRuntimeException) {
        logger.error("Could neither load nor register user", dex)
        throw BadCredentialsException("Internal error during authentication")
      }

  /**
   * Implementations should get user details by user id or create a new user if the user doesn't
   * exist and map with authentication details is provided.
   *
   * @param userId the id of the user to get user details for
   * @param map a map containing authentication details. If user is unknown and map is not null then
   * a new user can be created with data from map. Otherwise - if user is unknown and map is null a
   * BadClientCredentialsException should be thrown.
   * @return the user details
   */
  @Throws(AuthenticationException::class)
  abstract fun getUserDetails(userId: String?, map: Map<String, *>): UserDetails

  companion object {
    private const val SUBJECT_ENTRY = "sub"
    private const val BOSCH_ID_ENTRY = "bosch-id"
    private const val ISSUER_ENTRY = "iss"
    private const val NOT_AVAILABLE = "n/a"
    private val logger = getLogger(AbstractCustomUserAuthenticationConverter::class.java)
  }
}
