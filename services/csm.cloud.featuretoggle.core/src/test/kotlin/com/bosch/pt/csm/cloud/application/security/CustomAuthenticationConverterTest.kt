/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.application.security

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.security.CustomTrustedJwtIssuersProperties
import com.bosch.pt.csm.cloud.common.security.DefaultCustomUserAuthenticationConverter
import com.bosch.pt.csm.cloud.user.constants.RoleConstants
import com.bosch.pt.csm.cloud.user.query.UserProjection
import io.mockk.every
import io.mockk.impl.annotations.MockK
import java.net.URL
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.jwt.Jwt

@SmartSiteMockKTest
class CustomAuthenticationConverterTest {

  @MockK private lateinit var userDetailsService: UserDetailsService
  private lateinit var jwt: Jwt
  private lateinit var testUser: UserProjection
  private lateinit var cut: DefaultCustomUserAuthenticationConverter
  private lateinit var headers: MutableMap<String, Any>
  private lateinit var claims: MutableMap<String, Any>

  @BeforeEach
  fun initMocks() {
    cut =
        DefaultCustomUserAuthenticationConverter(
            userDetailsService, emptyList(), CustomTrustedJwtIssuersProperties(listOf(ISSUER)))
    testUser = UserProjection(UserId(IDENTIFIER), 0L, USERID_USER, true, false, null)

    val issuerUrl = URL(ISSUER)

    claims = HashMap()
    claims[ISSUER_ENTRY] = issuerUrl
    claims[SUBJECT_ENTRY] = USERID_USER
    claims[EMAIL_ENTRY] = EMAIL

    headers = HashMap()
    headers["typ"] = "JWT"
    headers["alg"] = "RS256"

    jwt = Jwt(JWT_VALUE, Instant.now(), Instant.now().plusSeconds(120), headers, claims)
  }

  @Test
  fun verifyConvertJwtAsAdmin() {
    every { userDetailsService.loadUserByUsername(USERID_USER) } returns testUser

    val authenticationToken = cut.convert(jwt)
    assertThat(authenticationToken).isNotNull
    assertThat(authenticationToken).isInstanceOf(UsernamePasswordAuthenticationToken::class.java)
    assertThat(authenticationToken.principal).isEqualTo(testUser)
    assertThat(authenticationToken.authorities)
        .containsExactlyInAnyOrder(
            SimpleGrantedAuthority(RoleConstants.USER.roleName()),
            SimpleGrantedAuthority(RoleConstants.ADMIN.roleName()))
  }

  @Test
  fun verifyConvertJwtAsUser() {
    testUser.admin = false
    every { userDetailsService.loadUserByUsername(USERID_USER) } returns testUser

    val authenticationToken = cut.convert(jwt)
    assertThat(authenticationToken).isNotNull
    assertThat(authenticationToken).isInstanceOf(UsernamePasswordAuthenticationToken::class.java)
    assertThat(authenticationToken.principal).isEqualTo(testUser)
    assertThat(authenticationToken.authorities)
        .containsExactlyInAnyOrder(SimpleGrantedAuthority(RoleConstants.USER.roleName()))
  }

  @Test
  fun verifyConvertJwtWithUnknownUserAndUnsupportedEmailType() {
    every { userDetailsService.loadUserByUsername(USERID_UNKNOWN) } throws
        UsernameNotFoundException("")

    claims[SUBJECT_ENTRY] = USERID_UNKNOWN
    claims[EMAIL_ENTRY] = Any()

    val jwt = Jwt(JWT_VALUE, Instant.now(), Instant.now().plusSeconds(120), headers, claims)
    assertThatThrownBy { cut.convert(jwt) }.isInstanceOf(UsernameNotFoundException::class.java)
  }

  @Test
  fun verifyConvertJwtWithStringIssuer() {
    testUser.admin = false
    claims[ISSUER_ENTRY] = ISSUER

    val jwt = Jwt(JWT_VALUE, Instant.now(), Instant.now().plusSeconds(120), headers, claims)
    every { userDetailsService.loadUserByUsername(USERID_USER) } returns testUser

    val authenticationToken = cut.convert(jwt)
    assertThat(authenticationToken).isNotNull
    assertThat(authenticationToken).isInstanceOf(UsernamePasswordAuthenticationToken::class.java)
    assertThat(authenticationToken.principal).isEqualTo(testUser)
    assertThat(authenticationToken.authorities)
        .containsExactlyInAnyOrder(SimpleGrantedAuthority(RoleConstants.USER.roleName()))
  }

  @Test
  fun verifyConvertJwtWithInvalidIssuer() {
    claims[ISSUER_ENTRY] = "invalid"
    val jwt = Jwt(JWT_VALUE, Instant.now(), Instant.now().plusSeconds(120), headers, claims)

    assertThatThrownBy { cut.convert(jwt) }.isInstanceOf(OAuth2AuthenticationException::class.java)
  }

  @Test
  fun verifyConvertJwtWithMissingIssuer() {
    claims.remove(ISSUER_ENTRY)
    val jwt = Jwt(JWT_VALUE, Instant.now(), Instant.now().plusSeconds(120), headers, claims)

    assertThatThrownBy { cut.convert(jwt) }.isInstanceOf(OAuth2AuthenticationException::class.java)
  }

  @Test
  fun verifyConvertJwtWithMissingSubjectEntry() {
    every { userDetailsService.loadUserByUsername(any()) } throws
        UsernameNotFoundException("not found")

    claims.remove(SUBJECT_ENTRY)
    val jwt = Jwt(JWT_VALUE, Instant.now(), Instant.now().plusSeconds(120), headers, claims)

    assertThatThrownBy { cut.convert(jwt) }.isInstanceOf(OAuth2AuthenticationException::class.java)
  }

  companion object {
    private val IDENTIFIER = UUID.randomUUID()
    private const val EMAIL = "hans.mustermann@example.com"
    private const val USERID_USER = EMAIL
    private const val USERID_UNKNOWN = "unknown"
    private const val ISSUER = "https://example.com"
    private const val SUBJECT_ENTRY = "sub"
    private const val ISSUER_ENTRY = "iss"
    private const val EMAIL_ENTRY = "email"
    private const val JWT_VALUE = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dC..."
  }
}
