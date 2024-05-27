/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security.facade.rest

import com.bosch.pt.iot.smartsite.api.ApiApplication
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(classes = [ApiApplication::class], webEnvironment = RANDOM_PORT)
@ActiveProfiles("test", "keycloak1")
class KeyCloak1SignupControllerTest(@Autowired private val webClient: WebTestClient) {

  @Test
  fun `signup redirects to login on for KEYCLOAK1`() =
      webClient
          .get()
          .uri("/signup?redirect_url=redirect_uri_value")
          .exchange()
          .expectHeader()
          .location("/login?redirect_url=redirect_uri_value")
          .expectStatus()
          .isFound
          .expectBody()
          .isEmpty
          .let {}
}
