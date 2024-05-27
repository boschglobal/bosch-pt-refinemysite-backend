/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.security

import org.springframework.security.core.userdetails.UserDetails

/**
 * Implementing beans will be wired to [AbstractCustomUserAuthenticationConverter]. Implementing
 * this interface allows to transform the [UserDetails] used as authentication information in the
 * security context based on information obtained from JWT claims.
 */
interface JwtVerificationListener {

  /**
   * @return either the [UserDetails] passed in or an updated instance of it that will be used in
   * the authentication context.
   */
  fun onJwtVerifiedEvent(jwtClaims: Map<String, *>, userDetails: UserDetails): UserDetails
}
