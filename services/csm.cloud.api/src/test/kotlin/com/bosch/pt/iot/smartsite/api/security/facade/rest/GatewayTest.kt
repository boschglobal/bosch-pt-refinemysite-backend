/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security.facade.rest

import com.bosch.pt.iot.smartsite.api.ApiApplication
import com.bosch.pt.iot.smartsite.api.security.config.SessionConfiguration.Companion.COOKIE_NAME
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.mockserver.client.MockServerClient
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType.APPLICATION_JSON
import org.mockserver.verify.VerificationTimes.atLeast
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    classes = [ApiApplication::class],
    webEnvironment = RANDOM_PORT,
    properties = ["mock-server-port=9123"]) // the port must be unique for this test class!)
@ActiveProfiles("test", "test-with-mock-server", "keycloak1")
@TestInstance(Lifecycle.PER_CLASS)
class GatewayTest(
    @Autowired private val webClient: WebTestClient,
    @Value("\${mock-server-port}") private val mockServerPort: Int,
) : AbstractSessionBasedAuthenticationIntegrationTest() {

  private lateinit var mockServerClient: MockServerClient

  val userRouteGetRequest: HttpRequest =
      request()
          .withMethod("GET")
          .withPath("/v3/users/current")
          .withHeader(AUTHORIZATION, "Bearer $tokenValue")

  val userRouteGetRequestWithFakeBearerToken: HttpRequest =
      request()
          .withMethod("GET")
          .withPath("/v3/users/current")
          .withHeader(AUTHORIZATION, "Bearer invalid-fake-token")

  val projectRoutePostRequest: HttpRequest =
      request()
          .withMethod("POST")
          .withPath("/v5/projects/db1ee7d2-f8e8-48a3-bc7d-fdcd6637d031/participants/search")
          .withQueryStringParameter("page")
          .withQueryStringParameter("size")
          .withBody("{\"status\": [ \"ACTIVE\" ], \"company\": null, \"roles\": []}")
          .withHeader(AUTHORIZATION, "Bearer $tokenValue")

  @BeforeAll
  fun startMockServer() {
    mockServerClient = ClientAndServer("localhost", mockServerPort, mockServerPort)
  }

  @BeforeEach
  fun setupMockServerExpectations() {
    mockServerClient.reset()
    mockServerClient
        .`when`(userRouteGetRequest)
        .respond(
            response()
                .withStatusCode(200)
                .withContentType(APPLICATION_JSON)
                .withBody("{ \"user\": \"fake\" }"))

    mockServerClient
        .`when`(userRouteGetRequestWithFakeBearerToken)
        .respond(
            response()
                .withStatusCode(401)
                .withContentType(APPLICATION_JSON)
                .withBody("{ \"reason\": \"invalid-token\" }"))

    mockServerClient
        .`when`(projectRoutePostRequest)
        .respond(
            response()
                .withStatusCode(200)
                .withContentType(APPLICATION_JSON)
                .withBody("{ \"search\": \"fake-result\" }"))
  }

  @Test
  fun `successfully proxies request to microservice with authorization from session`() {
    webClient
        .get()
        .uri("/v3/users/current")
        .cookie(COOKIE_NAME, session.id)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json("{ \"user\": \"fake\" }")
        .let {}
    logRequestsRecordedOnProxiedService()
    mockServerClient.verify(userRouteGetRequest, atLeast(1))
  }

  @Test
  fun `successfully proxies POST request to microservice with authorization from session`() {
    webClient
        .post()
        .uri(
            "/v5/projects/db1ee7d2-f8e8-48a3-bc7d-fdcd6637d031/participants/search?page=0&size=100")
        .bodyValue("{\"status\": [ \"ACTIVE\" ], \"company\": null, \"roles\": []}")
        .cookie(COOKIE_NAME, session.id)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json("{ \"search\": \"fake-result\" }")
        .let {}
    logRequestsRecordedOnProxiedService()
    mockServerClient.verify(projectRoutePostRequest, atLeast(1))
  }

  @Test
  fun `successfully proxies request to microservice with Authorization header without verification`() {
    webClient
        .get()
        .uri("/v3/users/current")
        .header(AUTHORIZATION, "Bearer $tokenValue")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json("{ \"user\": \"fake\" }")
        .let {}
    logRequestsRecordedOnProxiedService()
    mockServerClient.verify(userRouteGetRequest, atLeast(1))
  }

  @Test
  fun `successfully proxies POST request to microservice with Authorization header without verification`() {
    webClient
        .post()
        .uri(
            "/v5/projects/db1ee7d2-f8e8-48a3-bc7d-fdcd6637d031/participants/search?page=0&size=100")
        .bodyValue("{\"status\": [ \"ACTIVE\" ], \"company\": null, \"roles\": []}")
        .header(AUTHORIZATION, "Bearer $tokenValue")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json("{ \"search\": \"fake-result\" }")
        .let {}
    logRequestsRecordedOnProxiedService()
    mockServerClient.verify(projectRoutePostRequest, atLeast(1))
  }

  @Test
  fun `successfully proxies request to microservice with authorization header without verification`() {
    webClient
        .get()
        .uri("/v3/users/current")
        .header(AUTHORIZATION, "Bearer $tokenValue")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json("{ \"user\": \"fake\" }")
        .let {}
    logRequestsRecordedOnProxiedService()
    mockServerClient.verify(userRouteGetRequest, atLeast(1))
  }

  @Test
  fun `successfully proxies request to microservice with authorization from header prioritized over session`() {
    webClient
        .get()
        .uri("/v3/users/current")
        .cookie(COOKIE_NAME, session.id)
        .header(AUTHORIZATION, "Bearer invalid-fake-token")
        .exchange()
        .expectStatus()
        .isUnauthorized
        .expectBody()
        .json("{ \"reason\": \"invalid-token\" }")
        .let {}
    logRequestsRecordedOnProxiedService()
    mockServerClient.verify(userRouteGetRequestWithFakeBearerToken, atLeast(1))
  }

  private fun logRequestsRecordedOnProxiedService() {
    val logs = mockServerClient.retrieveRecordedRequestsAndResponses(null)
    LOGGER.info("mocked requests: ${logs.size}")
    for (log in logs) {
      LOGGER.info("mocked request: $log")
    }
  }

  @Test
  fun `fails for requests that require authentication when none is provided`() {
    webClient.get().uri("/v3/users/current").exchange().expectStatus().isUnauthorized.let {}
    mockServerClient.verifyZeroInteractions()
  }

  companion object {
    val LOGGER: Logger = getLogger(GatewayTest::class.java)
  }
}
