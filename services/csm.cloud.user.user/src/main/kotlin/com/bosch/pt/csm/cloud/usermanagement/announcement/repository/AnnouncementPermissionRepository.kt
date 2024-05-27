/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.announcement.repository

import com.bosch.pt.csm.cloud.usermanagement.announcement.model.AnnouncementPermission
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import org.springframework.data.jpa.repository.JpaRepository

interface AnnouncementPermissionRepository : JpaRepository<AnnouncementPermission, Long> {
  fun findOneByUser(user: User): AnnouncementPermission?
}
