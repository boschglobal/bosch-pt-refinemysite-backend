/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security.facade.rest

import com.bosch.pt.iot.smartsite.api.ApiApplication
import com.bosch.pt.iot.smartsite.api.security.Base64Utils
import com.bosch.pt.iot.smartsite.api.security.config.SessionConfiguration.Companion.COOKIE_NAME
import java.util.UUID.randomUUID
import org.hamcrest.core.StringContains
import org.hamcrest.core.StringStartsWith
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(classes = [ApiApplication::class], webEnvironment = RANDOM_PORT)
@ActiveProfiles("test", "keycloak1")
class AuthenticationControllerTest(@Autowired private val webClient: WebTestClient) :
    AbstractSessionBasedAuthenticationIntegrationTest() {

  @Test
  fun `login endpoint redirects to the redirect uri passed in encoded format after visiting the IdP`() =
      webClient
          .get()
          .uri("/login?redirect_url=aHR0cDovL2xvY2FsaG9zdDo4MDAwL3YxL2Fubm91bmNlbWVudHM=")
          .cookie(COOKIE_NAME, session.id)
          .exchange()
          .expectHeader()
          .location("http://localhost:8000/v1/announcements")
          .expectStatus()
          .isFound
          .expectBody()
          .isEmpty
          .let {}

    @Test
    fun `legacy login endpoint redirects to the new one`() =
        webClient
            .get()
            .uri("/api/login?redirect_url=aHR0cDovL2xvY2FsaG9zdDo4MDAwL3YxL2Fubm91bmNlbWVudHM=")
            .cookie(COOKIE_NAME, session.id)
            .exchange()
            .expectHeader()
            .location("/login?redirect_url=aHR0cDovL2xvY2FsaG9zdDo4MDAwL3YxL2Fubm91bmNlbWVudHM=")
            .expectStatus()
            .isPermanentRedirect
            .expectBody()
            .isEmpty
            .let {}

  @Test
  fun `login endpoint does not permit to redirect to arbitrary target endpoints`() {

    val redirectUrl =
        Base64Utils.encodeToUrlSafeString("https://www.example.com/evil/target".toByteArray())

    webClient
        .get()
        .uri("/login?redirect_url=$redirectUrl")
        .cookie(COOKIE_NAME, session.id)
        .exchange()
        .expectStatus()
        .isBadRequest
  }

  @Test
  fun `verify endpoint responds true with existing session`() =
      webClient
          .get()
          .uri("/login/verify")
          .cookie(COOKIE_NAME, session.id)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody(String::class.java)
          .isEqualTo("true")
          .let {}

  @Test
  fun `verify endpoint responds false without an existing session`() =
      webClient
          .get()
          .uri("/login/verify")
          .cookie(COOKIE_NAME, "${randomUUID()}")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody(String::class.java)
          .isEqualTo("false")
          .let {}

  @Test
  fun `logout endpoint redirects to the redirect uri passed in encoded format after visiting the IdP`() {
    webClient
        .get()
        .uri("/logout?redirect_url=aHR0cDovL2xvY2FsaG9zdDo4MDAwL3YxL2Fubm91bmNlbWVudHM=")
        .cookie(COOKIE_NAME, session.id)
        .exchange()
        .expectHeader()
        .value(
            "Location",
            StringStartsWith(
                "https://stage.key-cloak-one.com/auth/realms/central_profile/protocol/openid-connect/logout"))
        .expectHeader()
        .value("Location", StringContains("id_token_hint=ey"))
        .expectHeader()
        .value("Location", StringContains("post_logout_redirect_uri=http://localhost"))
        .expectStatus()
        .isTemporaryRedirect
        .expectBody()
        .isEmpty

    webClient
        .get()
        .uri("/logout/redirect")
        .cookie(COOKIE_NAME, session.id)
        .exchange()
        .expectHeader()
        .location("http://localhost:8000/v1/announcements")
        .expectStatus()
        .isFound
  }
}
