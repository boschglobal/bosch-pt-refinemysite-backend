/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.security

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.common.security.CustomTrustedJwtIssuersProperties
import com.bosch.pt.csm.cloud.common.security.DefaultCustomUserAuthenticationConverter
import com.bosch.pt.iot.smartsite.user.constants.RoleConstants.ADMIN
import com.bosch.pt.iot.smartsite.user.constants.RoleConstants.USER
import com.bosch.pt.iot.smartsite.user.model.GenderEnum.MALE
import com.bosch.pt.iot.smartsite.user.model.User
import io.mockk.every
import io.mockk.impl.annotations.MockK
import java.net.MalformedURLException
import java.net.URL
import java.time.Instant.now
import java.util.UUID.randomUUID
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
class CustomUserAuthenticationConverterTest {

  @MockK(relaxed = true) private lateinit var userDetailsService: UserDetailsService

  private val customTrustedJwtIssuersProperties: CustomTrustedJwtIssuersProperties =
      CustomTrustedJwtIssuersProperties(
          listOf("https://example.com", "https://other-issuer", "https://yet-another-issuer"))

  private lateinit var jwt: Jwt
  private lateinit var testUser: User
  private lateinit var cut: DefaultCustomUserAuthenticationConverter
  private lateinit var headers: MutableMap<String, Any>
  private lateinit var claims: MutableMap<String, Any>

  @BeforeEach
  @Throws(MalformedURLException::class)
  fun initMocks() {
    cut =
        DefaultCustomUserAuthenticationConverter(
            userDetailsService, emptyList(), customTrustedJwtIssuersProperties)

    testUser =
        User(
            IDENTIFIER,
            0L,
            USERID_USER,
            MALE,
            FIRST_NAME,
            LAST_NAME,
            EMAIL,
            POSITION,
            true,
            true,
            false,
            null,
            null,
            emptySet(),
            emptySet())

    claims =
        HashMap<String, Any>().apply {
          this[ISSUER_ENTRY] = URL(ISSUER)
          this[SUBJECT_ENTRY] = USERID_USER
          this[EMAIL_ENTRY] = EMAIL
        }

    headers =
        HashMap<String, Any>().apply {
          this["typ"] = "JWT"
          this["alg"] = "RS256"
        }

    jwt = Jwt(JWT_VALUE, now(), now().plusSeconds(120), headers, claims)
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
            SimpleGrantedAuthority(USER.roleName()), SimpleGrantedAuthority(ADMIN.roleName()))
  }

  @Test
  fun verifyConvertJwtAsUser() {
    every { userDetailsService.loadUserByUsername(USERID_USER) } returns testUser

    testUser.admin = false

    val authenticationToken = cut.convert(jwt)
    assertThat(authenticationToken).isNotNull
    assertThat(authenticationToken).isInstanceOf(UsernamePasswordAuthenticationToken::class.java)
    assertThat(authenticationToken.principal).isEqualTo(testUser)
    assertThat(authenticationToken.authorities)
        .containsExactlyInAnyOrder(SimpleGrantedAuthority(USER.roleName()))
  }

  @Test
  fun verifyConvertJwtWithUnknownUserAndUnsupportedEmailType() {
    every { userDetailsService.loadUserByUsername(USERID_UNKNOWN) } throws
        UsernameNotFoundException("User not found")

    claims[SUBJECT_ENTRY] = USERID_UNKNOWN
    claims[EMAIL_ENTRY] = Any()

    val jwt = Jwt(JWT_VALUE, now(), now().plusSeconds(120), headers, claims)

    assertThatThrownBy { cut.convert(jwt) }.isInstanceOf(UsernameNotFoundException::class.java)
  }

  @Test
  fun verifyConvertJwtWithStringIssuer() {
    testUser.admin = false
    claims[ISSUER_ENTRY] = ISSUER

    val jwt = Jwt(JWT_VALUE, now(), now().plusSeconds(120), headers, claims)

    every { userDetailsService.loadUserByUsername(USERID_USER) } returns testUser

    val authenticationToken = cut.convert(jwt)

    assertThat(authenticationToken).isNotNull
    assertThat(authenticationToken).isInstanceOf(UsernamePasswordAuthenticationToken::class.java)
    assertThat(authenticationToken.principal).isEqualTo(testUser)
    assertThat(authenticationToken.authorities)
        .containsExactlyInAnyOrder(SimpleGrantedAuthority(USER.roleName()))
  }

  @Test
  fun verifyConvertJwtWithInvalidIssuer() {
    claims[ISSUER_ENTRY] = "invalid"
    val jwt = Jwt(JWT_VALUE, now(), now().plusSeconds(120), headers, claims)

    assertThatThrownBy { cut.convert(jwt) }.isInstanceOf(OAuth2AuthenticationException::class.java)
  }

  @Test
  fun verifyConvertJwtWithMissingIssuer() {
    claims.remove(ISSUER_ENTRY)
    val jwt = Jwt(JWT_VALUE, now(), now().plusSeconds(120), headers, claims)

    assertThatThrownBy { cut.convert(jwt) }.isInstanceOf(OAuth2AuthenticationException::class.java)
  }

  @Test
  fun verifyConvertJwtWithMissingSubjectEntry() {
    every { userDetailsService.loadUserByUsername(any()) } throws
        UsernameNotFoundException("not found")

    claims.remove(SUBJECT_ENTRY)
    val jwt = Jwt(JWT_VALUE, now(), now().plusSeconds(120), headers, claims)

    assertThatThrownBy { cut.convert(jwt) }.isInstanceOf(OAuth2AuthenticationException::class.java)
  }

  companion object {
    private val IDENTIFIER = randomUUID()
    private const val EMAIL = "hans.mustermann@example.com"
    private const val USERID_USER = EMAIL
    private const val USERID_UNKNOWN = "unknown"
    private const val FIRST_NAME = "Hans"
    private const val LAST_NAME = "Mustermann"
    private const val ISSUER = "https://example.com"
    private const val SUBJECT_ENTRY = "sub"
    private const val ISSUER_ENTRY = "iss"
    private const val EMAIL_ENTRY = "email"
    private const val POSITION = "foreman"
    private const val JWT_VALUE = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dC..."
  }
}
