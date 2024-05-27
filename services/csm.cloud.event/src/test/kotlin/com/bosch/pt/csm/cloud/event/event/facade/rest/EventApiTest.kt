/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.event.event.facade.rest

import com.bosch.pt.csm.cloud.event.application.WebSecurityTestConfiguration
import com.bosch.pt.csm.cloud.event.application.WithMockSmartSiteUser
import com.bosch.pt.csm.cloud.event.application.config.LoggerConfiguration
import com.bosch.pt.csm.cloud.event.event.boundary.EventService
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.time.Duration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.http.codec.ServerSentEvent
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.FluxSink

@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@ActiveProfiles("test")
@AutoConfigureRestDocs(outputDir = "build/generated-snippets/events")
@AutoConfigureWebTestClient(timeout = "10000") // 10 seconds
@Import(WebSecurityTestConfiguration::class, LoggerConfiguration::class)
@ImportAutoConfiguration(OAuth2ResourceServerAutoConfiguration::class)
@WebFluxTest(EventController::class)
@WithMockSmartSiteUser
class EventApiTest {

  @Suppress("unused")
  @MockkBean(name = "eventUserDetails")
  private lateinit var userDetailsService: UserDetailsService

  @MockkBean private lateinit var eventService: EventService

  @Autowired private lateinit var webTestClient: WebTestClient

  private val eventTypeUpdate = "update"
  private val eventTypeNotification = "notification"
  private val eventTypeHeartbeat = "hb"

  private val dataUpdate =
      ("{\"root\":{\"type\":\"PROJECT\",\"identifier\":\"eeb28b69-4e17-8d22-952b-f92a025a892c\"}," +
          "\"object\":{\"type\":\"TOPIC\",\"identifier\":\"e26016c1-cd9f-9095-02b4-c29d77de002c\"," +
          "\"version\":0}, \"event\":\"CREATED\"}")

  private val dataNotification = "{\"lastAdded\":\"2019-08-26T06:41:48.625345Z\"}"

  @Suppress("UNCHECKED_CAST")
  @Test
  fun verifyGetEvent() {

    // Send a message when a user subscribes to SSEs
    every { eventService.subscribe(any(), any()) } answers
        {
          val sink = this.args[0] as FluxSink<Any>
          sink.next(ServerSentEvent.builder<Any>().event(eventTypeHeartbeat).data("{}").build())
          sink.next(ServerSentEvent.builder<Any>().event(eventTypeUpdate).data(dataUpdate).build())
          sink.next(
              ServerSentEvent.builder<Any>()
                  .event(eventTypeNotification)
                  .data(dataNotification)
                  .build())
        }

    // Subscribe to SSEs
    val result =
        webTestClient
            .get()
            .uri("/v1/events")
            .accept(TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(ServerSentEvent::class.java)

    // Create expected object to compare
    val objectMapper = ObjectMapper()
    val expectedNotification: ServerSentEvent<*> =
        ServerSentEvent.builder<Any>()
            .event(eventTypeNotification)
            .data(objectMapper.readValue(dataNotification, LinkedHashMap::class.java))
            .build()
    val expectedUpdate: ServerSentEvent<*> =
        ServerSentEvent.builder<Any>()
            .event(eventTypeUpdate)
            .data(objectMapper.readValue(dataUpdate, LinkedHashMap::class.java))
            .build()
    val expectedHeartbeat =
        ServerSentEvent.builder<Any>()
            .event(eventTypeHeartbeat)
            .data(objectMapper.readValue("{}", LinkedHashMap::class.java))
            .build()

    // Receive and compare with expected events
    val responseBody = result.responseBody
    val events = responseBody.take(3).collectList().block(Duration.ofSeconds(10L))!!

    assertThat(events).hasSize(3)
    assertThat(events[0]).usingRecursiveComparison().isEqualTo(expectedHeartbeat)
    assertThat(events[1]).usingRecursiveComparison().isEqualTo(expectedUpdate)
    assertThat(events[2]).usingRecursiveComparison().isEqualTo(expectedNotification)

    // Create api documentation
    result.consumeWith(
        document("get-events", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())))
  }
}
