/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.authorization

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.common.exceptions.ReferencedEntityNotFoundException
import com.bosch.pt.csm.common.i18n.Key.USER_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.csm.user.authorization.boundary.AdminUserAuthorizationService
import com.bosch.pt.csm.user.user.query.UserProjection
import com.bosch.pt.csm.user.user.query.UserProjectionRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component

@Component
class UserAuthorizer(
    private val adminUserAuthorizationService: AdminUserAuthorizationService,
    private val userProjectionRepository: UserProjectionRepository,
) {

  fun assertAuthorizedToAccessUser(userId: UserId) {
    userProjectionRepository.findOneById(userId).let {
      if (it == null || !adminUserAuthorizationService.authorizedForCountry(it.country)) {
        throw AccessDeniedException("Unauthorized to access employee of that user")
      }
    }
  }

  fun isUserAuthorizedForCountry(userProjection: UserProjection): Boolean =
      adminUserAuthorizationService.authorizedForCountry(userProjection.country)

  fun isUserAuthorizedForCountry(userId: UserId): Boolean =
      userProjectionRepository.findOneById(userId).let {
        if (it != null) isUserAuthorizedForCountry(it)
        else throw ReferencedEntityNotFoundException(USER_VALIDATION_ERROR_NOT_FOUND)
      }
}
