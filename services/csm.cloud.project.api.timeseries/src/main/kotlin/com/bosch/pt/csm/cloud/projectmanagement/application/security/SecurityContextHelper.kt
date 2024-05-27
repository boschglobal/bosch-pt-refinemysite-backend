/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder

object SecurityContextHelper {

  fun getCurrentUserDetails(): IdentifiableUserDetails {
    val authentication =
        getAuthentication()
            ?: throw InsufficientAuthenticationException("Fully authenticated user is required")

    return authentication.principal?.let { it as? IdentifiableUserDetails }
        ?: throw InsufficientAuthenticationException("Fully authenticated user is required")
  }

  private fun getAuthentication(): Authentication? =
      SecurityContextHolder.getContext().authentication?.let {
        if (it.isAuthenticated && it !is AnonymousAuthenticationToken) it else null
      }
}
