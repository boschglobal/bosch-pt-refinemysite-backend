package com.bosch.pt.csm.cloud.projectmanagement.event.model

import java.util.UUID

class Event(val receivers: Set<UUID>, val eventType: String, val message: String)
