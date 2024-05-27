/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.announcement.facade.rest.resources.factory

import com.bosch.pt.csm.cloud.usermanagement.announcement.facade.rest.resources.AnnouncementListResource
import com.bosch.pt.csm.cloud.usermanagement.announcement.model.AnnouncementTranslationProjection
import org.springframework.stereotype.Component

@Component
class AnnouncementListResourceFactory(
    private val announcementResourceFactory: AnnouncementResourceFactory
) {

  fun build(announcements: List<AnnouncementTranslationProjection>) =
      AnnouncementListResource(announcements.map { announcementResourceFactory.build(it) })
}
