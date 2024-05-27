/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.authorization

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.usermanagement.application.security.AuthorizationUtils
import com.bosch.pt.csm.cloud.usermanagement.application.security.SecurityContextHelper.getCurrentUser
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class UserAuthorizationComponent(
    @Value("\${system.user.identifier}") private val systemUserIdentifier: UserId
) {
  fun isCurrentUser(identifier: UserId) = getCurrentUser().identifier == identifier

  fun isCurrentUser(identifier: String) = AuthorizationUtils.isCurrentUser(identifier)

  fun isCurrentUserSystemUser() = isCurrentUser(systemUserIdentifier)
}
