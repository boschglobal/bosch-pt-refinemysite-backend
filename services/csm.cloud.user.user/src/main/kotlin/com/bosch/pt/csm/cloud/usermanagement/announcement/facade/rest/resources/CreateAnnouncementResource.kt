/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.announcement.facade.rest.resources

import com.bosch.pt.csm.cloud.usermanagement.announcement.model.AnnouncementTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.resource.CreateTranslationResource
import jakarta.validation.constraints.Size

data class CreateAnnouncementResource(
    val type: AnnouncementTypeEnum,
    @field:Size(min = 1) val translations: MutableSet<CreateTranslationResource> = HashSet()
)
