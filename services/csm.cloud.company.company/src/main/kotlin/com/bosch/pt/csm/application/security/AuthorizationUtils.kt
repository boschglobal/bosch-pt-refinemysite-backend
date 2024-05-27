/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.application.security

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.user.user.constants.RoleConstants.ADMIN
import com.bosch.pt.csm.user.user.constants.RoleConstants.USER
import com.bosch.pt.csm.user.user.query.UserProjection
import com.google.common.collect.Sets.newHashSet
import java.util.function.BiFunction
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder.getContext

/** Common authorization utils. */
object AuthorizationUtils {

  /**
   * Checks the authorization of the current [UserProjection] on an arbitrary target with help of a
   * query function. The query function is used to retrieve the target using a specifier (any
   * limiting parameter to the query function, for example a [UserId]) and the [UserProjection]'s
   * unique identifier. The function's first parameter must be the specifier, the second parameter
   * must be the user's identifier for which the authorization should be checked. The query function
   * returns the object on which the authorization is granted or null, if none exists. This function
   * will interpret any non-null result as a granted permission and will return true. If the result
   * is null, this method will return false. Exceptions are passed through.
   *
   * @param queryFunction query function to retrieve a target for which the user has access
   * authorization
   * @param specifier the target identifier
   * @param <T> the type of the target
   * @param <U> the type of the limiting specifier
   * @return true if authorization is granted, false otherwise </U></T>
   */
  fun <T, U> checkAuthorizationForSingleResult(
      queryFunction: BiFunction<U, UserId?, T?>,
      specifier: U
  ): Boolean {
    val principal = getCurrentUser()
    val target: T? = queryFunction.apply(specifier, principal.id)
    return target != null
  }

  /**
   * Checks the authorization of the current [UserProjection] on an arbitrary target with help of a
   * query function. The query function is used to retrieve the target using a specifier (any
   * limiting parameter to the query function, for example a [UserId]) and the [UserProjection]'s
   * unique identifier. The function's first parameter must be the specifier, the second parameter
   * must be the user's identifier for which the authorization should be checked. The query function
   * returns the object on which the authorization is granted or null, if none exists. This function
   * will interpret any non-null result as a granted permission and will return true. If the result
   * is null, this method will return false. Exceptions are passed through.
   *
   * @param queryFunction query function to retrieve a target for which the user has access
   * authorization
   * @param specifier the target identifier
   * @param <R> the type of the target
   * @param <U> the type of the limiting specifier
   * @return true if authorization is granted, false otherwise </U></R>
   */
  fun <R : Collection<UserId>?, U : Collection<UserId>> checkAuthorizationForMultipleResults(
      queryFunction: BiFunction<U, UserId?, R>,
      specifier: U
  ): Boolean {
    val principal = getCurrentUser()
    val target = queryFunction.apply(specifier, principal.id)
    return target != null && !target.isEmpty() && target.containsAll(specifier)
  }

  /**
   * Check if current [UserProjection] has the ADMIN role.
   *
   * @return true if current user is admin, false otherwise
   */
  fun hasRoleAdmin(): Boolean = SecurityContextHelper.hasRole(ADMIN.name)

  /**
   * Gets the currently signed on user.
   *
   * @return the currently signed on user.
   */
  fun getCurrentUser(): UserProjection = SecurityContextHelper.getCurrentUser()

  fun setUserAuthentication(user: UserProjection) {
    val roles: MutableCollection<SimpleGrantedAuthority> =
        newHashSet(SimpleGrantedAuthority(USER.roleName()))
    if (user.admin) {
      roles.add(SimpleGrantedAuthority(ADMIN.roleName()))
    }
    getContext().authentication = UsernamePasswordAuthenticationToken(user, null, roles)
  }
}
