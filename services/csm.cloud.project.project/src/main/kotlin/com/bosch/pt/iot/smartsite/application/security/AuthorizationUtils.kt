/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.security

import com.bosch.pt.iot.smartsite.user.constants.RoleConstants.ADMIN
import com.bosch.pt.iot.smartsite.user.constants.RoleConstants.USER
import com.bosch.pt.iot.smartsite.user.model.User
import java.util.UUID
import java.util.function.BiFunction
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

object AuthorizationUtils {

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
  fun <R : Collection<UUID>, U : Collection<UUID>> checkAuthorizationForMultipleResults(
      queryFunction: BiFunction<U, UUID, R?>,
      specifier: U
  ): Boolean {
    val principal = getCurrentUser()
    val target = queryFunction.apply(specifier, requireNotNull(principal.identifier))
    return target != null && !target.isEmpty() && target.containsAll(specifier)
  }

  /*
   * Check if current [User] has the ADMIN role.
   *
   * @return true if user is admin, false otherwise
   */
  fun hasRoleAdmin(): Boolean = SecurityContextHelper.getInstance().hasRole(ADMIN.name)

  /*
   * Gets the currently signed on user.
   *
   * @return user of the security context
   */
  fun getCurrentUser(): User = SecurityContextHelper.getInstance().getCurrentUser()

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
        mutableSetOf(SimpleGrantedAuthority(USER.roleName()))

    if (user.admin) {
      roles.add(SimpleGrantedAuthority(ADMIN.roleName()))
    }

    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, roles)
  }
}
