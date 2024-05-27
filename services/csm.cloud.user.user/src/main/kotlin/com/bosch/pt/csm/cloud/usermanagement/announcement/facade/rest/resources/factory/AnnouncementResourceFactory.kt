/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.announcement.facade.rest.resources.factory

import com.bosch.pt.csm.cloud.usermanagement.announcement.facade.rest.resources.AnnouncementResource
import com.bosch.pt.csm.cloud.usermanagement.announcement.model.AnnouncementTranslationProjection
import org.springframework.stereotype.Component

@Component
class AnnouncementResourceFactory {

  fun build(announcement: AnnouncementTranslationProjection) =
      AnnouncementResource(announcement.identifier, announcement.type, announcement.value)
}
