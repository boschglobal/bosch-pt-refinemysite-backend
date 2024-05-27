/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.application.security

import com.bosch.pt.csm.cloud.user.query.UserProjection
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder

/** Helper for evaluating roles and other user data from security context. */
object SecurityContextHelper {

  private const val DEFAULT_ROLE_PREFIX = "ROLE_"
  private const val FULL_AUTH_REQUIRED = "Fully authenticated user is required"

  /**
   * Checks that the current user has the given role.
   *
   * @param role roles to check
   * @return `true` if user has any of the given roles
   */
  fun hasRole(role: String): Boolean = hasAnyRole(role)

  /**
   * Checks that the current user has the given roles.
   *
   * @param roles roles to check
   * @return `true` if user has any of the given roles
   */
  fun hasAnyRole(vararg roles: String): Boolean = hasAnyAuthorityName(*roles)

  /**
   * Gets the current [User].
   *
   * @return the current user or null if not authenticated
   */
  fun getCurrentUser(): UserProjection {
    val authentication =
        getAuthentication() ?: throw InsufficientAuthenticationException(FULL_AUTH_REQUIRED)

    if (authentication.principal != null &&
        UserProjection::class.java != authentication.principal.javaClass) {
      throw InsufficientAuthenticationException(FULL_AUTH_REQUIRED)
    }

    return authentication.principal as UserProjection
  }

  private fun hasAnyAuthorityName(vararg roles: String): Boolean {
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
  private fun getRoleWithDefaultPrefix(role: String?): String? {
    if (role == null) {
      return null
    }
    return if (role.startsWith(DEFAULT_ROLE_PREFIX)) {
      role
    } else DEFAULT_ROLE_PREFIX + role
  }

  private fun getAuthoritySet(): Set<String?> =
      if (this.getAuthentication() == null) {
        HashSet()
      } else {
        val userAuthorities = requireNotNull(this.getAuthentication()).authorities
        AuthorityUtils.authorityListToSet(userAuthorities)
      }

  private fun getAuthentication(): Authentication? {
    val authentication = SecurityContextHolder.getContext().authentication
    return if (authentication != null &&
        authentication.isAuthenticated &&
        authentication !is AnonymousAuthenticationToken) {
      authentication
    } else {
      null
    }
  }
}
