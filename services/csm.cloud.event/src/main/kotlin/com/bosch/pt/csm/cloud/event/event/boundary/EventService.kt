package com.bosch.pt.csm.cloud.event.event.boundary

import com.bosch.pt.csm.cloud.event.event.boundary.handler.EventMessageHandler
import com.bosch.pt.csm.cloud.event.event.boundary.handler.HeartbeatMessageHandler
import com.bosch.pt.csm.cloud.event.user.model.User
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.Metrics
import java.util.concurrent.atomic.AtomicInteger
import kotlin.streams.asSequence
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.codec.ServerSentEvent
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.integration.channel.PublishSubscribeChannel
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.FluxSink

@Service
class EventService(
    @Qualifier("broadcastingChannel") private val broadcastingChannel: PublishSubscribeChannel,
    @Qualifier("heartbeatChannel") private val heartbeatChannel: PublishSubscribeChannel,
    private val objectMapper: ObjectMapper
) {

  private val connections =
      checkNotNull(Metrics.gauge("custom.api.event.connections.count", AtomicInteger(0)))

  fun subscribe(sink: FluxSink<ServerSentEvent<*>>, user: User) {
    val eventMessageHandler =
        EventMessageHandler(sink, user, objectMapper).also { broadcastingChannel.subscribe(it) }
    val heartbeatMessageHandler =
        HeartbeatMessageHandler(sink).also { heartbeatChannel.subscribe(it) }

    val remoteHost = getRemoveHost(sink)
    val userIdentifier = user.identifier

    connections.incrementAndGet()
    sink.onDispose {
      connections.decrementAndGet()
      broadcastingChannel.unsubscribe(eventMessageHandler)
      heartbeatChannel.unsubscribe(heartbeatMessageHandler)
    }
  }

  private fun getRemoveHost(sink: FluxSink<ServerSentEvent<*>>) =
      sink
          .contextView()
          .stream()
          .asSequence()
          .filter {
            it.key is Class<*> && (it.key as Class<*>).name == ServerWebExchange::class.java.name
          }
          .map { it.value as ServerHttpRequest }
          .firstOrNull()
          ?.remoteAddress
          ?.hostString

  companion object {
    private val LOGGER = LoggerFactory.getLogger(EventService::class.java)
  }
}
