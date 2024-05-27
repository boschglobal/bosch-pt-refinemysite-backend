/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model

import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.details.PatUserDetailsAuthenticationToken
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.domain.asPatId
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatProjection
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.service.PatQueryService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCrypt

class RmsPat1AuthenticationTokenConverter(private val patQueryService: PatQueryService) :
    Converter<PatToken, AbstractAuthenticationToken> {

  companion object {
    private val logger: Logger =
        LoggerFactory.getLogger(RmsPat1AuthenticationTokenConverter::class.java)
  }

  override fun convert(token: PatToken): AbstractAuthenticationToken = extractAuthentication(token)

  private fun extractAuthentication(token: PatToken): PatUserDetailsAuthenticationToken =
      try {
        getPat(token).let { PatUserDetailsAuthenticationToken(it, it.authorities) }
      } catch (ex: UsernameNotFoundException) {
        logger.info("Could not load user", ex)
        throw ex
      }

  private fun getPat(token: PatToken): PatProjection =
      when {
        token.secret.isBlank() -> throw UsernameNotFoundException("Empty secret given")
        else -> {
          val pat =
              patQueryService.findByIdentifier(token.patId.asPatId())
                  ?: throw UsernameNotFoundException("No user found")
          val isValid = BCrypt.checkpw(token.toString(), pat.hash)
          if (!isValid) {
            throw UsernameNotFoundException("No pat found")
          }
          if (!pat.isAccountNonLocked) {
            throw LockedException(
                "The user account with userId ${pat.impersonatedUserIdentifier} is locked")
          }
          if (!pat.isCredentialsNonExpired) {
            throw CredentialsExpiredException("Pat expired")
          }
          pat
        }
      }
}
