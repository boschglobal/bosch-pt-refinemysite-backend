/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.announcement.repository

import com.bosch.pt.csm.cloud.usermanagement.announcement.model.Announcement
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface AnnouncementRepository : JpaRepository<Announcement, Long> {
  fun deleteByIdentifier(identifier: UUID)
}
