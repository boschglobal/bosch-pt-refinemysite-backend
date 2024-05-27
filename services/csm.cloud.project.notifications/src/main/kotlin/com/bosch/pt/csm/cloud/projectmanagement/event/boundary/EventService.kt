/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.event.boundary

import com.bosch.pt.csm.cloud.projectmanagement.event.integration.EventIntegrationService
import com.bosch.pt.csm.cloud.projectmanagement.event.integration.message.Event
import com.bosch.pt.csm.cloud.projectmanagement.event.model.NotificationUpdateEvent
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import datadog.trace.api.Trace
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class EventService(
    private val objectMapper: ObjectMapper,
    private val eventIntegrationService: EventIntegrationService,
) {

  @Trace
  fun send(user: UUID, timestamp: Instant) =
      try {
        val jsonMessage =
            objectMapper.writer().writeValueAsString(NotificationUpdateEvent(timestamp))
        val event = Event(setOf(user), "notification", jsonMessage)
        eventIntegrationService.send(event)
      } catch (e: JsonProcessingException) {
        throw IllegalStateException(e)
      }
}
