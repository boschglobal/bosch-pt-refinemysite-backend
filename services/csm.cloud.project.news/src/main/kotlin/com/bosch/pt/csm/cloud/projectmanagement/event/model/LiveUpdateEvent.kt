package com.bosch.pt.csm.cloud.projectmanagement.event.model

import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import com.fasterxml.jackson.annotation.JsonProperty

class LiveUpdateEvent(
    val root: ObjectIdentifier,
    @JsonProperty("object") val objectIdentifier: ObjectIdentifierWithVersion,
    val event: String
)
