/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.event.integration.message

import java.util.UUID

data class Event(val receivers: Set<UUID>, val eventType: String, val message: String)
