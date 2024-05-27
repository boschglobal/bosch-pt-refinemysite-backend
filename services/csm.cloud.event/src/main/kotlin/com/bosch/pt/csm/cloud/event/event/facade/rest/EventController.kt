package com.bosch.pt.csm.cloud.event.event.facade.rest

import com.bosch.pt.csm.cloud.event.event.boundary.EventService
import com.bosch.pt.csm.cloud.event.user.model.User
import org.springframework.http.codec.ServerSentEvent
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink

@RestController
class EventController(private val eventService: EventService) {

  @GetMapping(value = ["/v1/events"])
  fun getEvents(@AuthenticationPrincipal user: User): Flux<ServerSentEvent<*>> =
      Flux.create { sink: FluxSink<ServerSentEvent<*>> -> eventService.subscribe(sink, user) }
}
