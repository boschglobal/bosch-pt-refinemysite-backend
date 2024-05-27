/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.details

import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatProjection
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

/**
 * Implementation similar to
 * [org.springframework.security.authentication.UsernamePasswordAuthenticationToken] for PATs.
 */
class PatUserDetailsAuthenticationToken(
    val pat: PatProjection,
    authorities: Collection<GrantedAuthority>
) : AbstractAuthenticationToken(authorities) {

  companion object {
    private const val NOT_AVAILABLE = "n/a"
  }

  init {
    // Initialized as authenticated, if initialized with authorities
    super.setAuthenticated(authorities.isNotEmpty())
  }

  override fun getCredentials(): String = NOT_AVAILABLE

  override fun getPrincipal(): PatProjection = pat

  override fun setAuthenticated(authenticated: Boolean) {
    require(!authenticated) {
      "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead"
    }
    super.setAuthenticated(false)
  }
}
