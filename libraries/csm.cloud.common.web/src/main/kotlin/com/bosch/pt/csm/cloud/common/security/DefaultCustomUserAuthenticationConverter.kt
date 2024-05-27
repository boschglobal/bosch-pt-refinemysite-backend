/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.security

import org.slf4j.LoggerFactory
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException

/**
 * Simple implementation of [AbstractCustomUserAuthenticationConverter] that will use
 * [UserDetailsService] to determine [UserDetails]. Will be autowired by
 * [CustomWebSecurityAutoConfiguration] if no specific implementation is provided.
 */
class DefaultCustomUserAuthenticationConverter(
    private val userDetailsService: UserDetailsService,
    jwtVerificationListeners: List<JwtVerificationListener>,
    trustedJwtIssuersProperties: CustomTrustedJwtIssuersProperties
) : AbstractCustomUserAuthenticationConverter(jwtVerificationListeners, trustedJwtIssuersProperties) {

  override fun getUserDetails(userId: String?, map: Map<String, *>): UserDetails =
      try {
        userDetailsService.loadUserByUsername(userId)
      } catch (ex: UsernameNotFoundException) {
        logger.info("Could not load user", ex)
        throw ex
      }

  companion object {
    private val logger =
        LoggerFactory.getLogger(DefaultCustomUserAuthenticationConverter::class.java)
  }
}
