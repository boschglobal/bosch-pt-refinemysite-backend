/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource
import java.time.Instant

class NotificationListResource(
    val items: List<NotificationResource>,
    val lastSeen: Instant? = null
) : AbstractResource() {
  companion object {
    const val LINK_PREVIOUS = "prev"
  }
}
