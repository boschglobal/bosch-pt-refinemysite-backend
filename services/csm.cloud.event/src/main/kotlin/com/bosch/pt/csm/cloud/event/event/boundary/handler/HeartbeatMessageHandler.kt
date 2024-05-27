package com.bosch.pt.csm.cloud.event.event.boundary.handler

import org.slf4j.LoggerFactory
import org.springframework.http.codec.ServerSentEvent
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHandler
import org.springframework.messaging.MessagingException
import reactor.core.publisher.FluxSink

class HeartbeatMessageHandler(private val sink: FluxSink<ServerSentEvent<*>>) : MessageHandler {

  @Throws(MessagingException::class)
  override fun handleMessage(message: Message<*>) {
    sink.next(ServerSentEvent.builder<Any>().event("hb").build())
    LOGGER.debug("Heartbeat sent to client")
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(HeartbeatMessageHandler::class.java)
  }
}
