/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.sideeffect

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.usermanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.usermanagement.application.config.EnableAllKafkaListeners
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractApiIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitProfilePicture
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import io.mockk.every
import io.mockk.impl.annotations.MockK
import java.net.URL
import java.time.Instant
import java.util.UUID.randomUUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.hateoas.MediaTypes
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoderFactory
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
@SmartSiteSpringBootTest
@EnableAllKafkaListeners
class UpdateEmailApiIntegrationTest : AbstractApiIntegrationTest() {

  @Autowired private lateinit var mockMvc: MockMvc

  @Autowired private lateinit var jwtDecoderFactory: JwtDecoderFactory<String>

  @MockK private lateinit var decoder: JwtDecoder

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitUser("user").submitProfilePicture("picture")
    userEventStoreUtils.reset()
  }

  @Test
  fun `verify that the user's email is updated when there is a changed email in the JWT`() {

    val user = getUserFromRepository()
    val initialUserEmail = user.email

    mockJwtContainingChangedEmail(user)

    mockMvc
        .perform(get(latestVersionOfCurrentUserEndpoint()).header(AUTHORIZATION, BEARER_TOKEN))
        .andExpect(status().isOk)
        .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
        .andExpect(jsonPath(EMAIL_KEY).value(CHANGED_EMAIL))

    val actualUserEmail = getUserFromRepository().email

    assertNotEquals(initialUserEmail, actualUserEmail)
    assertEquals(CHANGED_EMAIL, actualUserEmail)
  }

  private fun getUserFromRepository(): User =
      requireNotNull(
          repositories.userRepository.findOneByIdentifier(
              UserId(eventStreamGenerator.getIdentifier("user"))))

  private fun mockJwtContainingChangedEmail(user: User) {

    val headers: MutableMap<String, Any> = HashMap()
    val claims: MutableMap<String, Any> = HashMap()

    headers["typ"] = "JWT"
    headers["alg"] = "RS256"

    claims[ISSUER_KEY] = URL(ISSUER)
    claims[SUBJECT_KEY] = randomUUID()
    claims[BOSCH_ID] = user.externalUserId!!
    claims[EMAIL_KEY] = CHANGED_EMAIL

    every { jwtDecoderFactory.createDecoder(ISSUER) }.returns(decoder)
    every { decoder.decode(any()) }
        .returns(Jwt(JWT_VALUE, Instant.now(), Instant.now().plusSeconds(120), headers, claims))
  }

  private fun latestVersionOfCurrentUserEndpoint(): String =
      "/v" + apiVersionProperties.version.max + "/users/current"

  companion object {
    private const val EMAIL_KEY = "email"
    private const val ISSUER_KEY = "iss"
    private const val SUBJECT_KEY = "sub"
    private const val BOSCH_ID = "bosch-id"
    private const val ISSUER = "https://test-issuer.com"
    private const val CHANGED_EMAIL = "got-changed@example.com"
    private const val JWT_VALUE = "eyJ0eXAiOiJK..."

    private const val AUTHORIZATION = "Authorization"
    // bearer token with payload { "iss": "https://test-issuer.com" } created on jwt.io
    // which gets us past CustomTrustedIssuerJwtAuthenticationManagerResolver
    private const val BEARER_TOKEN =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJpc3MiOiJodHRwczovL3Rlc3QtaXNzdWVyLmNvbSJ9." +
            "mz2tiAkvKzVjSksyXZoo1a7o44U4xdvuiv7ab4bLUZ8"
  }
}
