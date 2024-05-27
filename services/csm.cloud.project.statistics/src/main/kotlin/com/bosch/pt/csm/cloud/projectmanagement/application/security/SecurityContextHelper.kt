/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.User
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder

/** Helper for evaluating roles and other user data from security context. */
class SecurityContextHelper private constructor() {
  /**
   * Checks that the current user has the given role.
   *
   * @param role roles to check
   * @return `true` if user has any of the given roles
   */
  fun hasRole(role: String): Boolean {
    return hasAnyRole(role)
  }

  /**
   * Checks that the current user has the given roles.
   *
   * @param roles roles to check
   * @return `true` if user has any of the given roles
   */
  fun hasAnyRole(vararg roles: String): Boolean {
    return hasAnyAuthorityName(*roles)
  }

  /**
   * Gets the current [User].
   *
   * @return the current user or null if not authenticated
   */
  val currentUser: User
    get() {
      val authentication =
          authentication
              ?: throw InsufficientAuthenticationException("Fully authenticated user is required")
      if (authentication.principal != null &&
          User::class.java != authentication.principal.javaClass) {
        throw InsufficientAuthenticationException("Fully authenticated user is required")
      }
      return authentication.principal as User
    }

  private fun hasAnyAuthorityName(vararg roles: String): Boolean {
    val roleSet = authoritySet
    for (role in roles) {
      val defaultedRole = getRoleWithDefaultPrefix(role)
      if (roleSet.contains(defaultedRole)) {
        return true
      }
    }
    return false
  }

  /**
   * Prefixes role with defaultRolePrefix if defaultRolePrefix is non-null and if role does not
   * already start with defaultRolePrefix.
   *
   * @param role the role
   * @return the prefixed role
   */
  private fun getRoleWithDefaultPrefix(role: String?): String? {
    if (role == null) {
      return null
    }
    return if (role.startsWith(DEFAULT_ROLE_PREFIX)) {
      role
    } else DEFAULT_ROLE_PREFIX + role
  }

  private val authoritySet: Set<String>
    get() {
      val authentication = authentication
      return if (authentication == null) {
        HashSet()
      } else {
        val userAuthorities = authentication.authorities
        AuthorityUtils.authorityListToSet(userAuthorities)
      }
    }
  private val authentication: Authentication?
    get() {
      val authentication = SecurityContextHolder.getContext().authentication
      return if (authentication != null &&
          authentication.isAuthenticated &&
          authentication !is AnonymousAuthenticationToken) {
        authentication
      } else {
        null
      }
    }

  companion object {
    private const val DEFAULT_ROLE_PREFIX = "ROLE_"

    /**
     * Gets the singleton instance for [SecurityContextHelper].
     *
     * @return the singleton instance
     */
    val instance = SecurityContextHelper()
  }
}
