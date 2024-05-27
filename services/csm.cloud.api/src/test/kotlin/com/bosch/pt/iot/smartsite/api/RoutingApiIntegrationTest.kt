/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api

import com.bosch.pt.iot.smartsite.api.security.config.SessionConfiguration.Companion.COOKIE_NAME
import com.bosch.pt.iot.smartsite.api.security.facade.rest.AbstractSessionBasedAuthenticationIntegrationTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.cloud.contract.spec.internal.HttpStatus.OK
import org.springframework.http.HttpStatus.EXPECTATION_FAILED
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.StatusAssertions
import org.springframework.test.web.reactive.server.WebTestClient

@ActiveProfiles("test", "test-with-mock-server", "keycloak1")
@SpringBootTest(
    classes = [ApiApplication::class],
    webEnvironment = RANDOM_PORT,
    properties = ["mock-server-port=9128"]) // the port must be unique for this test class!)
@AutoConfigureWebTestClient(timeout = "10000") // 10 seconds
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoutingApiIntegrationTest(
    @Value("\${mock-server-port}") private val mockServerPort: Int,
    @Autowired private var webTestClient: WebTestClient
) : AbstractSessionBasedAuthenticationIntegrationTest() {

  @BeforeAll
  fun startMockServer() {
    ClientAndServer("localhost", mockServerPort, mockServerPort).apply {
      // for all requests, respond with OK
      `when`(request()).respond(response().withStatusCode(OK))
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings =
          [
              "/timeline/v1",
              "/timeline/v1/",
              "/timeline/v1/companies",
              "/timeline/v1/projects",
              "/timeline/v1/translations",
              "/timeline/v1/users"])
  fun `verify timeline routes`(uri: String) {
    assertThatHttpStatus(uri).isOk
  }

  @ParameterizedTest
  @ValueSource(strings = ["/graphql/v1", "/graphql/v1/"])
  fun `verify graphql routes`(uri: String) {
    assertThatHttpStatus(uri).isOk
  }

  @ParameterizedTest
  @ValueSource(
      strings =
          [
              "/v1/announcements",
              "/v2/companies",
              "/v1/crafts",
              "/v1/documents",
              "/v1/events",
              "/v1/features",
              "/v1/jobs",
              "/v5/projects",
              "/v3/users"])
  fun `verify internal routes`(uri: String) {
    assertThatHttpStatus(uri).isOk
  }

  @ParameterizedTest
  @ValueSource(
      strings =
          [
              "/internal/v1/announcements",
              "/internal/v2/companies",
              "/internal/v1/crafts",
              "/internal/v1/documents",
              "/internal/v1/events",
              "/internal/v1/features",
              "/internal/v1/jobs",
              "/internal/v5/projects",
              "/internal/v3/users"])
  fun `verify internal routes prefixed with internal`(uri: String) {
    assertThatHttpStatus(uri).isOk
  }

  @Test
  fun `verify swagger route`() {
    assertThatHttpStatus("/swagger/activities").isOk
  }

  @Test
  fun `verify timeline route with unsupported version fails`() {
    assertThatHttpStatus("/timeline/v99").isEqualTo(EXPECTATION_FAILED)
  }

  @Test
  fun `verify graphql route with unsupported version fails`() {
    assertThatHttpStatus("/graphql/v99").isEqualTo(EXPECTATION_FAILED)
  }

  @Test
  fun `verify internal route with unsupported version fails`() {
    assertThatHttpStatus("/v99/users/").isEqualTo(EXPECTATION_FAILED)
  }

  private fun assertThatHttpStatus(uri: String): StatusAssertions =
      webTestClient.get().uri(uri).cookie(COOKIE_NAME, session.id).exchange().expectStatus()
}
