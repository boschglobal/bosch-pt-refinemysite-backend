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
import org.mockserver.model.MediaType
import org.mockserver.verify.VerificationTimes
import org.mockserver.verify.VerificationTimes.exactly
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpStatus.EXPECTATION_FAILED
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    classes = [ApiApplication::class],
    webEnvironment = RANDOM_PORT,
    properties = ["mock-server-port=9127"]) // the port must be unique for this test class!)
@ActiveProfiles("test", "test-with-mock-server", "keycloak1")
@TestInstance(Lifecycle.PER_CLASS)
class ApiVersionGatewayTest(
    @Autowired private val webClient: WebTestClient,
    @Value("\${mock-server-port}") private val mockServerPort: Int,
) : AbstractSessionBasedAuthenticationIntegrationTest() {

  private lateinit var mockServerClient: MockServerClient

  val userRouteGetRequest: HttpRequest =
      request()
          .withMethod("GET")
          .withPath("/v3/users/current")
          .withHeader("Authorization", "Bearer $tokenValue")

  val timelineProjectGetRequest: HttpRequest =
      request()
          .withMethod("GET")
          .withPath("/v1/projects")
          .withHeader("X-Forwarded-Prefix", "/timeline")
          .withHeader("Authorization", "Bearer $tokenValue")

  val projectGetRequest: HttpRequest =
      request()
          .withMethod("GET")
          .withPath("/v5/projects")
          .withHeader("Authorization", "Bearer $tokenValue")

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
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("{ \"user\": \"fake\" }"))

    mockServerClient
        .`when`(projectGetRequest)
        .respond(
            response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("{ \"projects\": \"fake-projects-result\" }"))

    mockServerClient
        .`when`(timelineProjectGetRequest)
        .respond(
            response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("{ \"timeline\": \"fake-projects-result\" }"))
  }

  @Test
  fun `proxies request with permitted internal version`() {
    webClient
        .get()
        .uri("/internal/v5/projects")
        .cookie(COOKIE_NAME, session.id)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json("{ \"projects\": \"fake-projects-result\" }")
    logRequestsRecordedOnProxiedService()
    mockServerClient.verify(projectGetRequest, exactly(1))
  }

  @Test
  fun `proxies request with timeline specific version`() {
    webClient
        .get()
        .uri("/timeline/v1/projects")
        .cookie(COOKIE_NAME, session.id)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json("{ \"timeline\": \"fake-projects-result\" }")
    logRequestsRecordedOnProxiedService()
    mockServerClient.verify(timelineProjectGetRequest, exactly(1))
  }

  @Test
  fun `denies request with unsupported high timeline specific version`() {
    webClient
        .get()
        .uri("/timeline/v2/projects")
        .cookie(COOKIE_NAME, session.id)
        .exchange()
        .expectStatus()
        .isEqualTo(EXPECTATION_FAILED)
        .expectBody()
        .json(
            "{\"message\":\"API version not supported. Supported versions are 1 to 1.\",\"traceId\":\"0\"}")
    logRequestsRecordedOnProxiedService()
    mockServerClient.verifyZeroInteractions()
  }

  @Test
  fun `proxies request to microservice with supported version`() {
    webClient
        .get()
        .uri("/v3/users/current")
        .header("Authorization", "Bearer $tokenValue")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json("{ \"user\": \"fake\" }")
        .let {}
    logRequestsRecordedOnProxiedService()
    mockServerClient.verify(userRouteGetRequest, VerificationTimes.atLeast(1))
  }

  @Test
  fun `proxies request under internal route to microservice with supported version`() {
    webClient
        .get()
        .uri("/internal/v3/users/current")
        .header("Authorization", "Bearer $tokenValue")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json("{ \"user\": \"fake\" }")
        .let {}
    logRequestsRecordedOnProxiedService()
    mockServerClient.verify(userRouteGetRequest, VerificationTimes.atLeast(1))
  }

  @Test
  fun `denies request to microservice with unsupported high version`() {
    webClient
        .get()
        .uri("/v4/users/current")
        .header("Authorization", "Bearer $tokenValue")
        .exchange()
        .expectStatus()
        .isEqualTo(417)
        .expectBody()
        .json(
            "{\"message\":\"API version not supported. Supported versions are 3 to 3.\",\"traceId\":\"0\"}")
        .let {}
    logRequestsRecordedOnProxiedService()
    mockServerClient.verifyZeroInteractions()
  }

  @Test
  fun `denies request under internal route to microservice with unsupported high version`() {
    webClient
        .get()
        .uri("/internal/v4/users/current")
        .header("Authorization", "Bearer $tokenValue")
        .exchange()
        .expectStatus()
        .isEqualTo(417)
        .expectBody()
        .json(
            "{\"message\":\"API version not supported. Supported versions are 3 to 3.\",\"traceId\":\"0\"}")
        .let {}
    logRequestsRecordedOnProxiedService()
    mockServerClient.verifyZeroInteractions()
  }

  @Test
  fun `denies request to microservice with unsupported low version`() {
    webClient
        .get()
        .uri("/v2/users/current")
        .header("Authorization", "Bearer $tokenValue")
        .exchange()
        .expectStatus()
        .isEqualTo(417)
        .expectBody()
        .json(
            "{\"message\":\"API version not supported. Supported versions are 3 to 3.\",\"traceId\":\"0\"}")
        .let {}
    logRequestsRecordedOnProxiedService()
    mockServerClient.verifyZeroInteractions()
  }

  @Test
  fun `denies request under internal route to microservice with unsupported low version`() {
    webClient
        .get()
        .uri("/internal/v2/users/current")
        .header("Authorization", "Bearer $tokenValue")
        .exchange()
        .expectStatus()
        .isEqualTo(EXPECTATION_FAILED)
        .expectBody()
        .json(
            "{\"message\":\"API version not supported. Supported versions are 3 to 3.\",\"traceId\":\"0\"}")
        .let {}
    logRequestsRecordedOnProxiedService()
    mockServerClient.verifyZeroInteractions()
  }

  private fun logRequestsRecordedOnProxiedService() {
    val logs = mockServerClient.retrieveRecordedRequestsAndResponses(null)
    LOGGER.info("mocked requests: ${logs.size}")
    for (log in logs) {
      LOGGER.info("mocked request: $log")
    }
  }

  companion object {
    val LOGGER: Logger = getLogger(ApiVersionGatewayTest::class.java)
  }
}
