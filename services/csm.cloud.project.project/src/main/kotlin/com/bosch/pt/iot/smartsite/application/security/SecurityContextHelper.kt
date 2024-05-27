/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.security

import com.bosch.pt.iot.smartsite.user.model.User
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
  fun hasRole(role: String?): Boolean = hasAnyRole(role)

  /**
   * Checks that the current user has the given roles.
   *
   * @param roles roles to check
   * @return `true` if user has any of the given roles
   */
  fun hasAnyRole(vararg roles: String?): Boolean = hasAnyAuthorityName(*roles)

  /**
   * Gets the current [User].
   *
   * @return the current user or null if not authenticated
   */
  fun getCurrentUser(): User {
    val authentication =
        getAuthentication()
            ?: throw InsufficientAuthenticationException("Fully authenticated user is required")

    if (authentication.principal != null &&
        User::class.java != authentication.principal.javaClass) {
      throw InsufficientAuthenticationException("Fully authenticated user is required")
    }

    return authentication.principal as User
  }

  private fun hasAnyAuthorityName(vararg roles: String?): Boolean {
    val roleSet = getAuthoritySet()

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
  private fun getRoleWithDefaultPrefix(role: String?): String? =
      role?.let { if (role.startsWith(DEFAULT_ROLE_PREFIX)) role else DEFAULT_ROLE_PREFIX + role }

  private fun getAuthoritySet(): Set<String> =
      getAuthentication()?.let { AuthorityUtils.authorityListToSet(it.authorities) } ?: HashSet()

  private fun getAuthentication(): Authentication? =
      SecurityContextHolder.getContext().authentication?.let {
        if (it.isAuthenticated && it !is AnonymousAuthenticationToken) it else null
      }

  companion object {

    private const val DEFAULT_ROLE_PREFIX = "ROLE_"

    /**
     * Gets the singleton instance for [SecurityContextHelper].
     *
     * @return the singleton instance
     */
    @JvmStatic fun getInstance() = SecurityContextHelper()
  }
}
