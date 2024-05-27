/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.event.model

import java.time.Instant

data class NotificationUpdateEvent(val lastAdded: Instant)
