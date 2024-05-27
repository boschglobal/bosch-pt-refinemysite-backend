/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.application.security

import com.bosch.pt.csm.cloud.usermanagement.application.security.SecurityContextHelper.getCurrentUser
import com.bosch.pt.csm.cloud.usermanagement.user.user.UserRoleEnum
import com.bosch.pt.csm.cloud.usermanagement.user.user.UserRoleEnum.ADMIN
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import java.util.UUID
import java.util.function.BiFunction
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

object AuthorizationUtils {

  /*
   * Runs a block as a given user and restores the previous authentication configuration in the end.
   *
   * @param user the given user to run the block unit
   * @param block the block of unit to run
   */
  fun doWithAuthenticatedUser(user: User, block: () -> Unit) {
    val previousAuthentication = SecurityContextHolder.getContext().authentication

    setUserAuthentication(user)
    block()

    SecurityContextHolder.getContext().authentication = previousAuthentication
  }

  /*
   * Sets the corresponding user authentication to the security context.
   *
   * @param user the given user to set to the authentication
   */
  private fun setUserAuthentication(user: User) {
    val roles: MutableCollection<SimpleGrantedAuthority> =
        mutableSetOf(SimpleGrantedAuthority(UserRoleEnum.USER.roleName()))

    if (user.admin) {
      roles.add(SimpleGrantedAuthority(ADMIN.roleName()))
    }

    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, roles)
  }

  /**
   * Checks the authorization of the current [User] on an arbitrary target with help of a query
   * function. The query function is used to retrieve the target using a specifier (any limiting
   * parameter to the query function, for example a [UUID]) and the [User]'s unique identifier. The
   * function's first parameter must be the specifier, the second parameter must be the user's
   * identifier for which the authorization should be checked. The query function returns the object
   * on which the authorization is granted or null, if none exists. This function will interpret any
   * non-null result as a granted permission and will return true. If the result is null, this
   * method will return false. Exceptions are passed through.
   *
   * @param queryFunction query function to retrieve a target for which the user has access
   *   authorization
   * @param specifier the target identifier
   * @param <T> the type of the target
   * @param <U> the type of the limiting specifier
   * @return true if authorization is granted, false otherwise </U></T>
   */
  fun <T, U> checkAuthorizationForSingleResult(
      queryFunction: BiFunction<U, UUID?, T>,
      specifier: U
  ): Boolean {
    val principal = getCurrentUser()
    val target: T? = queryFunction.apply(specifier, principal.getIdentifierUuid())
    return target != null
  }

  /**
   * Checks the authorization of the current [User] on an arbitrary target with help of a query
   * function. The query function is used to retrieve the target using a specifier (any limiting
   * parameter to the query function, for example a [UUID]) and the [User]'s unique identifier. The
   * function's first parameter must be the specifier, the second parameter must be the user's
   * identifier for which the authorization should be checked. The query function returns the object
   * on which the authorization is granted or null, if none exists. This function will interpret any
   * non-null result as a granted permission and will return true. If the result is null, this
   * method will return false. Exceptions are passed through.
   *
   * @param queryFunction query function to retrieve a target for which the user has access
   *   authorization
   * @param specifier the target identifier
   * @param <R> the type of the target
   * @param <U> the type of the limiting specifier
   * @return true if authorization is granted, false otherwise </U></R>
   */
  fun <R : Collection<UUID>?, U : Collection<UUID>> checkAuthorizationForMultipleResults(
      queryFunction: BiFunction<U, UUID?, R>,
      specifier: U
  ): Boolean {
    val principal = getCurrentUser()
    val target = queryFunction.apply(specifier, principal.getIdentifierUuid())
    return target != null && !target.isEmpty() && target.containsAll(specifier)
  }

  /**
   * Check if current [User] has the ADMIN role.
   *
   * @return true if current user is admin, false otherwise
   */
  fun hasRoleAdmin(): Boolean = SecurityContextHelper.hasRole(ADMIN.name)

  fun isCurrentUser(externalUserId: String): Boolean =
      SecurityContextHelper.getCurrentUserDetails().username == externalUserId
}
