/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoderFactory
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

@TestPropertySource(
    properties = ["custom.security.oauth2.resourceserver.jwt.issuer-uris=https://test-issuer.com"])
@SpringJUnitConfig(classes = [WebSecurityTestConfiguration::class])
class OAuthIntegrationTest : AuthenticationBaseIntegrationTest() {

  private val fakeToken =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
          "eyJpc3MiOiJodHRwczovL3Rlc3QtaXNzdWVyLmNvbSJ9." +
          "mz2tiAkvKzVjSksyXZoo1a7o44U4xdvuiv7ab4bLUZ8"

  @Autowired private lateinit var jwtDecoderFactory: JwtDecoderFactory<String>

  @Autowired private lateinit var jwtDecoder: JwtDecoder

  @Autowired private lateinit var jwt: Jwt

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitProject().submitCsmParticipant()
  }

  @AfterEach
  fun clean() {
    clearMocks(jwtDecoder, jwtDecoderFactory)
  }

  @Test
  fun `graphql api with oauth`() {
    val user = get<UserAggregateAvro>("csm-user")!!

    mockJwtDecoder(user)

    val response = callGraphQlApi("Bearer $fakeToken").block()
    assertThat(response).contains(getIdentifier("project").toString())
  }

  @Test
  fun `timeline api with oauth`() {
    val user = get<UserAggregateAvro>("csm-user")!!

    mockJwtDecoder(user)

    val response = callTimelineApi("Bearer $fakeToken").block()
    assertThat(response).contains(getIdentifier("project").toString())
  }

  private fun mockJwtDecoder(user: UserAggregateAvro) {
    every { jwtDecoderFactory.createDecoder(any()) } returns jwtDecoder
    every { jwtDecoder.decode(any()) } returns jwt
    every { jwt.claims } returns
        mapOf("iss" to "https://test-issuer.com", "sub" to user.userId, "bosch-id" to user.userId)
  }
}

@TestConfiguration
@Profile("test")
class WebSecurityTestConfiguration {

  @Bean fun jwtDecoderFactory(): JwtDecoderFactory<String> = mockk(relaxed = true)

  @Bean fun jwtDecoder(): JwtDecoder = mockk(relaxed = true)

  @Bean fun jwt(): Jwt = mockk(relaxed = true)
}
