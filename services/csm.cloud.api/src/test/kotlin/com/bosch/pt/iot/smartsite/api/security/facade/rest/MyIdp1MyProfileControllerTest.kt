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
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(classes = [ApiApplication::class], webEnvironment = RANDOM_PORT)
@ActiveProfiles("test", "keycloak1")
class MyIdp1MyProfileControllerTest(@Autowired private val webClient: WebTestClient) :
    AbstractSessionBasedAuthenticationIntegrationTest() {

  @Test
  fun `supports legacy change-password endpoint for web-frontend`() =
      webClient
          .get()
          .uri("/change-password?redirect_url=redirect_uri_value")
          .cookie(COOKIE_NAME, session.id)
          .exchange()
          .expectHeader()
          .location("https://stage.my-idp-one.com/myprofile/")
          .expectStatus()
          .isFound
          .expectBody()
          .isEmpty
          .let {}

  @Test
  fun `profile endpoint redirects to MYIDP1 myprofile page`() =
      webClient
          .get()
          .uri("/profile")
          .cookie(COOKIE_NAME, session.id)
          .exchange()
          .expectHeader()
          .location("https://stage.my-idp-one.com/myprofile/")
          .expectStatus()
          .isFound
          .expectBody()
          .isEmpty
          .let {}
}
