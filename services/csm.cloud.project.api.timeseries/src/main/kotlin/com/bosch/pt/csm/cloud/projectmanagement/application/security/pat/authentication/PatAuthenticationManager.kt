/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication

import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.PatAuthenticationToken
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.PatTypeConverter
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatTypeEnum
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationManagerResolver
import org.springframework.security.core.Authentication

class PatAuthenticationManager(private val resolver: AuthenticationManagerResolver<PatTypeEnum>) :
    AuthenticationManager {

  private val typeConverter = PatTypeConverter()

  override fun authenticate(authentication: Authentication?): Authentication {
    require(authentication is PatAuthenticationToken) {
      "Authentication must be of type PatAuthenticationToken"
    }
    val type = typeConverter.convert(authentication)
    val authenticationManager =
        resolver.resolve(type) ?: throw InvalidPatException("Invalid pat type")
    return authenticationManager.authenticate(authentication)
  }
}
