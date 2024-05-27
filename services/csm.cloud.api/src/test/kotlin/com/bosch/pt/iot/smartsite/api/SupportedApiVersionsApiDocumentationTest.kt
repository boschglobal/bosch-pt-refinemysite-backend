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
import com.bosch.pt.iot.smartsite.api.versioning.ApiVersionProperties
import java.time.Duration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.FluxExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient

@ExtendWith(RestDocumentationExtension::class, SpringExtension::class)
@ActiveProfiles("test", "keycloak1")
@AutoConfigureRestDocs
@AutoConfigureWebTestClient(timeout = "10000") // 10 seconds
@SpringBootTest(classes = [ApiApplication::class], webEnvironment = RANDOM_PORT)
class SupportedApiVersionsApiDocumentationTest :
    AbstractSessionBasedAuthenticationIntegrationTest() {

  @Autowired private lateinit var webTestClient: WebTestClient

  @Autowired private lateinit var apiVersionProperties: ApiVersionProperties

  @Test
  fun `verify and document supported api versions`() {
    val result =
        webTestClient
            .get()
            .uri("/v1/versions")
            .accept(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(ApiVersionProperties::class.java)

    val supportedVersions = result.responseBody.blockFirst(Duration.ofSeconds(10L))
    assertThat(supportedVersions).isEqualTo(apiVersionProperties)

    // Document api
    result.consumeWith(
        document<FluxExchangeResult<ApiVersionProperties>>(
            "supported-versions/get-supported-api-versions",
            preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint())))
  }

  @Test
  fun `verify supported api versions when authenticated`() {
    val result =
        webTestClient
            .get()
            .uri("/v1/versions")
            .cookie(COOKIE_NAME, session.id)
            .accept(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(ApiVersionProperties::class.java)

    val supportedVersions = result.responseBody.blockFirst(Duration.ofSeconds(10L))
    assertThat(supportedVersions).isEqualTo(apiVersionProperties)
  }
}
