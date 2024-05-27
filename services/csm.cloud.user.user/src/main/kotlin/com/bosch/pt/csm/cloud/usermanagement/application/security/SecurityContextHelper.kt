/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.application.security

import com.bosch.pt.csm.cloud.usermanagement.user.user.UserRoleEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails

/** Helper for evaluating roles and other user data from security context. */
object SecurityContextHelper {

  private const val DEFAULT_ROLE_PREFIX = "ROLE_"
  private const val FULL_AUTH_REQUIRED = "Fully authenticated user is required"

  fun <T> executeAuthenticatedAs(user: User, block: () -> T): T {
    // Get original authentication
    val originalAuthentication = SecurityContextHolder.getContext().authentication

    try {
      // Set the given user temporarily into the authentication context
      setAuthenticationContext(user)

      // Invoke the block to execute
      return block.invoke()
    } finally {
      // Restore the original authentication context
      SecurityContextHolder.getContext().authentication = originalAuthentication
    }
  }

  fun setAuthenticationContext(user: User) {
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(
            user, "N/A", setOf(SimpleGrantedAuthority(USER.roleName())))
  }

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
  fun getCurrentUser(): User {
    val authentication =
        authentication ?: throw InsufficientAuthenticationException(FULL_AUTH_REQUIRED)

    if (authentication.principal != null &&
        User::class.java != authentication.principal.javaClass) {
      throw InsufficientAuthenticationException(FULL_AUTH_REQUIRED)
    }

    return authentication.principal as User
  }

  fun getCurrentUserDetails(): UserDetails {
    val authentication =
        authentication ?: throw InsufficientAuthenticationException(FULL_AUTH_REQUIRED)

    if (authentication.principal != null && authentication.principal !is UserDetails) {
      throw InsufficientAuthenticationException(FULL_AUTH_REQUIRED)
    }

    return authentication.principal as UserDetails
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

    return when (role.startsWith(DEFAULT_ROLE_PREFIX)) {
      true -> role
      else -> DEFAULT_ROLE_PREFIX + role
    }
  }

  private val authoritySet: Set<String?>
    get() =
        when (authentication == null) {
          true -> HashSet()
          else -> AuthorityUtils.authorityListToSet(authentication!!.authorities)
        }

  private val authentication: Authentication?
    get() =
        SecurityContextHolder.getContext().authentication.let {
          return when (it != null && it.isAuthenticated && it !is AnonymousAuthenticationToken) {
            true -> it
            else -> null
          }
        }
}
