/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token

import org.springframework.security.authentication.AbstractAuthenticationToken

class PatAuthenticationToken(
    val token: String,
) : AbstractAuthenticationToken(emptyList()) {

  companion object {
    private const val NOT_AVAILABLE = "n/a"
  }

  override fun getCredentials(): String = NOT_AVAILABLE

  override fun getPrincipal(): String = token
}
