package com.bosch.pt.csm.cloud.event.event.boundary.handler

import com.bosch.pt.csm.cloud.event.user.model.User
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.codec.ServerSentEvent
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHandler
import org.springframework.messaging.MessagingException
import org.springframework.messaging.support.GenericMessage
import reactor.core.publisher.FluxSink

class EventMessageHandler(
    private val sink: FluxSink<ServerSentEvent<*>>,
    private val user: User,
    private val objectMapper: ObjectMapper
) : MessageHandler {

  @Throws(MessagingException::class)
  override fun handleMessage(message: Message<*>) {
    val event = filterMessage(message, user)
    if (event != null) {
      val sse: ServerSentEvent<*> =
          ServerSentEvent.builder<String>()
              .event(event["eventType"] as String)
              .data(serializeMessage(event))
              .build()
      sink.next(sse)
    }
  }

  private fun serializeMessage(event: Map<*, *>): String =
      try {
        val message = event["message"]
        if (message is String) {
          message
        } else {
          objectMapper.writeValueAsString(message)
        }
      } catch (e: JsonProcessingException) {
        throw IllegalStateException(e)
      }

  private fun filterMessage(message: Message<*>, user: User): Map<*, *>? {
    require(message is GenericMessage<*>) { "Unexpected message type" }

    require(message.payload is Map<*, *>) { "Unexpected payload type" }
    val payload = message.getPayload() as Map<*, *>

    val receiversObject = payload["receivers"]
    require(
        receiversObject is Collection<*>,
    ) {
      "Unexpected receivers type"
    }

    return if (receiversObject.contains(user.identifier.toString())) {
      payload
    } else null
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(EventMessageHandler::class.java)
  }
}
