/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
@file:Suppress("SwallowedException")

package com.bosch.pt.csm.cloud.usermanagement.application.security

import com.bosch.pt.csm.cloud.common.security.AbstractCustomUserAuthenticationConverter
import com.bosch.pt.csm.cloud.common.security.CustomTrustedJwtIssuersProperties
import com.bosch.pt.csm.cloud.common.security.JwtVerificationListener
import com.bosch.pt.csm.cloud.usermanagement.user.user.UserRoleEnum.ADMIN
import com.bosch.pt.csm.cloud.usermanagement.user.user.UserRoleEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.UnregisteredUser
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory.getLogger
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils.createAuthorityList
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException

/**
 * Similar to the default [CustomUserAuthenticationConverter], but adds authorities specific to user
 * service.
 */
class CustomUserAuthenticationConverter(
    private val userDetailsService: UserDetailsService,
    jwtVerificationListeners: List<JwtVerificationListener>,
    trustedJwtIssuersProperties: CustomTrustedJwtIssuersProperties
) :
    AbstractCustomUserAuthenticationConverter(
        jwtVerificationListeners, trustedJwtIssuersProperties) {

  /**
   * Get user details by user id or create a new user if the user doesn't exist and map with
   * authentication details is provided.
   *
   * @param userId the id of the user to get user details for
   * @param map a map containing authentication details. If user is unknown and map is not null then
   * a new user will be created with data from map. Otherwise - if user is unknown and map is null
   * BadClientCredentialsException is thrown.
   * @return the user details
   */
  @Throws(AuthenticationException::class)
  override fun getUserDetails(userId: String?, map: Map<String, *>): UserDetails =
      try {
        val user = userDetailsService.loadUserByUsername(userId) as User
        user.authorities = getAuthorities(user.admin)
        user
      } catch (ex: UsernameNotFoundException) {

        val emailObject = map[EMAIL_ENTRY]
        if (emailObject != null && emailObject !is String) {
          LOGGER.info("Authentication with invalid details found.")
          throw BadCredentialsException("Authentication with invalid details found")
        }

        val email = StringUtils.defaultIfBlank(emailObject as String?, NOT_AVAILABLE)!!
        UnregisteredUser(userId!!, email)
      }

  private fun getAuthorities(isAdmin: Boolean): Collection<GrantedAuthority> =
      when (isAdmin) {
        true -> createAuthorityList(USER.roleName(), ADMIN.roleName())
        else -> createAuthorityList(USER.roleName())
      }

  companion object {
    private const val EMAIL_ENTRY = "email"
    private const val NOT_AVAILABLE = "n/a"
    private val LOGGER = getLogger(CustomUserAuthenticationConverter::class.java)
  }
}
