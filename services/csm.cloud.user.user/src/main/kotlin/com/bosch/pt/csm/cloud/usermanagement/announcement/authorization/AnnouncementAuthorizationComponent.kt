/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.announcement.authorization

import com.bosch.pt.csm.cloud.usermanagement.announcement.repository.AnnouncementPermissionRepository
import com.bosch.pt.csm.cloud.usermanagement.application.security.SecurityContextHelper
import com.bosch.pt.csm.cloud.usermanagement.application.security.SecurityContextHelper.getCurrentUser
import com.bosch.pt.csm.cloud.usermanagement.user.user.UserRoleEnum.ADMIN
import org.springframework.stereotype.Component

@Component
class AnnouncementAuthorizationComponent(
    private val announcementPermissionRepository: AnnouncementPermissionRepository
) {
  fun hasMaintainAnnouncementsPermission() =
      hasRoleAdmin() || announcementPermissionRepository.findOneByUser(getCurrentUser()) != null

  companion object {
    private fun hasRoleAdmin() = SecurityContextHelper.hasRole(ADMIN.name)
  }
}
