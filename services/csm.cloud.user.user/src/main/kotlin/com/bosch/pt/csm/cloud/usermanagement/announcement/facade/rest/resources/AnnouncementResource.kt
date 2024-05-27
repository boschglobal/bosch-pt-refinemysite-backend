/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.announcement.facade.rest.resources

import com.bosch.pt.csm.cloud.usermanagement.announcement.model.AnnouncementTypeEnum
import java.util.UUID

data class AnnouncementResource(
    val id: UUID,
    val type: AnnouncementTypeEnum,
    val message: String
)
