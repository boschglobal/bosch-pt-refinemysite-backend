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
import java.util.Date

data class LastSeenResource(val lastSeen: Instant? = null) : AbstractResource()

data class UpdateLastSeenResource(val lastSeen: Date)
