/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.LastSeenResource
import java.time.Instant
import org.springframework.stereotype.Component

@Suppress("UNREACHABLE_CODE")
@Component
class LastSeenResourceFactory {

  fun build(date: Instant?): LastSeenResource = LastSeenResource(lastSeen = date)
}
