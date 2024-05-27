/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.security

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.oauth2.jwt.JwtDecoderFactory

@ExtendWith(MockKExtension::class)
class CustomTrustedIssuerJwtAuthenticationManagerResolverTest {

  @MockK private lateinit var customTrustedJwtIssuersProperties: CustomTrustedJwtIssuersProperties

  @MockK private lateinit var authenticationConverter: DefaultCustomUserAuthenticationConverter

  @MockK(relaxed = true) private lateinit var jwtDecoderFactory: JwtDecoderFactory<String>

  private lateinit var cut: CustomTrustedIssuerJwtAuthenticationManagerResolver

  @BeforeEach
  fun init() {
    cut =
        CustomTrustedIssuerJwtAuthenticationManagerResolver(
            customTrustedJwtIssuersProperties, authenticationConverter, jwtDecoderFactory)

    every { customTrustedJwtIssuersProperties.issuerUris } returns
        listOf("https://p36.authz.bosch.com/auth/realms/central_profile")
  }

  @Test
  fun resolvesTrustedManagerSuccessfully() {
    val issuer = customTrustedJwtIssuersProperties.issuerUris.first()
    val authenticationManager = cut.resolve(issuer)
    assertNotNull(authenticationManager)
  }

  @Test
  fun resolvesNullForUntrustedIssuerUri() {
    val issuer = "https://untrusted-uri"
    val authenticationManager = cut.resolve(issuer)
    assertNull(authenticationManager)
  }
}
