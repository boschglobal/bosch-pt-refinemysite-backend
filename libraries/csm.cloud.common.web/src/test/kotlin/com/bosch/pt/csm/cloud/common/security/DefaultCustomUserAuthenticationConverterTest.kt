/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.security

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import java.util.Locale
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.jwt.Jwt

@ExtendWith(MockKExtension::class)
internal class DefaultCustomUserAuthenticationConverterTest {

  @MockK private lateinit var userDetailsService: UserDetailsService

  @MockK private lateinit var verificationListener: JwtVerificationListener

  private lateinit var cut: DefaultCustomUserAuthenticationConverter

  @BeforeEach
  fun init() {
    cut =
        DefaultCustomUserAuthenticationConverter(
            userDetailsService,
            listOf(verificationListener),
            CustomTrustedJwtIssuersProperties(listOf("https://issuer1", "https://issuer2")))
  }

  @Test
  fun verifyConvertFailedInvalidIssuer() {
    val untrustedIssuer = randomUUID().toString() as Any
    val claims = mapOf("iss" to untrustedIssuer)
    val headers = mapOf("fakeHeader" to randomUUID() as Any)
    val jwt = Jwt(randomUUID().toString(), null, null, headers, claims)

    assertThatExceptionOfType(OAuth2AuthenticationException::class.java)
        .isThrownBy { cut.convert(jwt) }
        .usingComparator(
            Comparator.comparing { ex: OAuth2AuthenticationException -> ex.error.errorCode })
        .isEqualTo(OAuth2AuthenticationException(OAuth2Error("Untrusted issuer $untrustedIssuer")))
    verify(exactly = 0) { verificationListener.onJwtVerifiedEvent(any(), any()) }
  }

  @Test
  fun verifyConvertFailedUsernameNotFound() {
    val claims = HashMap<String, Any>()
    claims["iss"] = ISSUER
    claims["sub"] = randomUUID().toString()
    val headers = mapOf("fakeHeader" to randomUUID() as Any)
    val jwt = Jwt(randomUUID().toString(), null, null, headers, claims)

    every { userDetailsService.loadUserByUsername(any()) } throws
        UsernameNotFoundException("not found")

    assertThatExceptionOfType(UsernameNotFoundException::class.java).isThrownBy { cut.convert(jwt) }
    verify(exactly = 0) { verificationListener.onJwtVerifiedEvent(any(), any()) }
  }

  @Test
  fun verifyConvertUser() {
    val claims = HashMap<String, Any>()
    claims["iss"] = ISSUER
    claims["sub"] = randomUUID().toString()

    val headers = mapOf("fakeHeader" to randomUUID() as Any)
    val jwt = Jwt(randomUUID().toString(), null, null, headers, claims)

    val user = TestUser(randomUUID().toString(), locked = false, locale = Locale.UK)

    every { userDetailsService.loadUserByUsername(any()) } returns user

    val claimsSlot = slot<Map<String, *>>()
    val userDetailsSlot = slot<UserDetails>()
    every {
      verificationListener.onJwtVerifiedEvent(capture(claimsSlot), capture(userDetailsSlot))
    } answers { userDetailsSlot.captured }

    val authentication = cut.convert(jwt)
    assertThat(authentication).isNotNull
    assertThat(authentication.principal).isEqualTo(user)
    assertThat(authentication.authorities)
        .containsAll(AuthorityUtils.createAuthorityList("testUserAuthority"))
  }

  @Test
  fun verifyDenyTokenByUntrustedIssuer() {
    val claims = HashMap<String, Any>()
    claims["iss"] = UNTRUSTED_ISSUER
    claims["sub"] = randomUUID().toString()

    val headers = mapOf("fakeHeader" to randomUUID() as Any)
    val jwt = Jwt(randomUUID().toString(), null, null, headers, claims)

    val user = TestUser(randomUUID().toString(), locked = false, locale = Locale.UK)

    every { userDetailsService.loadUserByUsername(any()) } returns user

    assertThatExceptionOfType(OAuth2AuthenticationException::class.java).isThrownBy {
      cut.convert(jwt)
    }
    verify(exactly = 0) { verificationListener.onJwtVerifiedEvent(any(), any()) }
  }

  companion object {
    private const val ISSUER = "https://issuer2"
    private const val UNTRUSTED_ISSUER = "https://issuer3"
  }
}
