/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.User

object AuthorizationUtils {

  /**
   * Check if current [User] has the ADMIN role.
   *
   * @return true if current user is admin, false otherwise
   */
  fun hasRoleAdmin() = SecurityContextHelper.instance.hasRole(RoleConstants.ADMIN.name)

  /**
   * Gets the currently signed on user.
   *
   * @return the currently signed on user.
   */
  fun getCurrentUser(): User = SecurityContextHelper.instance.currentUser
}
