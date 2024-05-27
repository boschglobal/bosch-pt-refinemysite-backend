/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.application.security

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.common.security.CustomTrustedJwtIssuersProperties
import com.bosch.pt.csm.cloud.usermanagement.application.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.usermanagement.user.user.GenderEnum.MALE
import com.bosch.pt.csm.cloud.usermanagement.user.user.UserRoleEnum.ADMIN
import com.bosch.pt.csm.cloud.usermanagement.user.user.UserRoleEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.handler.UpdateUserCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.UnregisteredUser
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import io.mockk.every
import io.mockk.impl.annotations.MockK
import java.net.MalformedURLException
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.jwt.Jwt

@SmartSiteMockKTest
class CustomUserAuthenticationConverterTest {

  @MockK private lateinit var userDetailsService: UserDetailsService

  @Suppress("UnusedPrivateMember")
  @MockK
  private lateinit var userCommandHandler: UpdateUserCommandHandler

  private lateinit var jwt: Jwt
  private lateinit var cut: CustomUserAuthenticationConverter

  private var headers: MutableMap<String, Any> = HashMap()
  private var claims: MutableMap<String, Any> = HashMap()

  private var testUser: User =
      User(
              UserId(randomUUID()),
              USERID_USER,
              MALE,
              FIRST_NAME,
              LAST_NAME,
              EMAIL,
              Locale.UK,
              IsoCountryCodeEnum.GB,
              LocalDate.now())
          .apply { admin = true }

  @BeforeEach
  @Throws(MalformedURLException::class)
  fun initMocks() {
    val issuers = CustomTrustedJwtIssuersProperties(listOf("https://example.com"))
    cut = CustomUserAuthenticationConverter(userDetailsService, emptyList(), issuers)

    claims[ISSUER_ENTRY] = URL(ISSUER)
    claims[SUBJECT_ENTRY] = USERID_USER
    claims[EMAIL_ENTRY] = EMAIL

    headers["typ"] = "JWT"
    headers["alg"] = "RS256"

    jwt = Jwt(JWT_VALUE, Instant.now(), Instant.now().plusSeconds(120), headers, claims)
  }

  @Test
  fun verifyConvertJwtAsAdmin() {
    every { userDetailsService.loadUserByUsername(USERID_USER) } returns
        testUser.apply { admin = true }
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
    every { userDetailsService.loadUserByUsername(USERID_USER) } returns
        testUser.apply { admin = false }

    val authenticationToken = cut.convert(jwt)
    assertThat(authenticationToken).isNotNull
    assertThat(authenticationToken).isInstanceOf(UsernamePasswordAuthenticationToken::class.java)
    assertThat(authenticationToken.principal).isEqualTo(testUser)
    assertThat(authenticationToken.authorities)
        .containsExactlyInAnyOrder(SimpleGrantedAuthority(USER.roleName()))
  }

  @Test
  fun verifyConvertJwtWithUnregisteredUser() {
    every { userDetailsService.loadUserByUsername(USERID_UNKNOWN) } throws
        UsernameNotFoundException("User not found")

    claims[SUBJECT_ENTRY] = USERID_UNKNOWN
    val jwt = Jwt(JWT_VALUE, Instant.now(), Instant.now().plusSeconds(120), headers, claims)

    val authenticationToken = cut.convert(jwt)
    assertThat(authenticationToken).isNotNull
    assertThat(authenticationToken).isInstanceOf(UsernamePasswordAuthenticationToken::class.java)
    assertThat(authenticationToken.principal).isNotNull
    assertThat(authenticationToken.principal).isInstanceOf(UnregisteredUser::class.java)
    assertThat((authenticationToken.principal as UnregisteredUser).email).isEqualTo(EMAIL)
    assertThat(authenticationToken.authorities).isEmpty()
  }

  @Test
  fun verifyConvertJwtWithUnregisteredUserAndNoEmail() {
    every { userDetailsService.loadUserByUsername(USERID_UNKNOWN) } throws
        UsernameNotFoundException("User not found")

    claims[SUBJECT_ENTRY] = USERID_UNKNOWN
    claims.remove(EMAIL_ENTRY)
    val jwt = Jwt(JWT_VALUE, Instant.now(), Instant.now().plusSeconds(120), headers, claims)

    val authenticationToken = cut.convert(jwt)
    assertThat(authenticationToken).isNotNull
    assertThat(authenticationToken).isInstanceOf(UsernamePasswordAuthenticationToken::class.java)
    assertThat(authenticationToken.principal).isNotNull
    assertThat(authenticationToken.principal).isInstanceOf(UnregisteredUser::class.java)
    assertThat((authenticationToken.principal as UnregisteredUser).email).isEqualTo("n/a")
    assertThat(authenticationToken.authorities).isEmpty()
  }

  @Test
  fun verifyConvertJwtWithUnknownUserAndUnsupportedEmailType() {
    every { userDetailsService.loadUserByUsername(USERID_UNKNOWN) } throws
        UsernameNotFoundException("User not found")

    claims[SUBJECT_ENTRY] = USERID_UNKNOWN
    claims[EMAIL_ENTRY] = Any()
    val jwt = Jwt(JWT_VALUE, Instant.now(), Instant.now().plusSeconds(120), headers, claims)
    assertThatThrownBy { cut.convert(jwt) }.isInstanceOf(BadCredentialsException::class.java)
  }

  @Test
  fun verifyConvertJwtWithStringIssuer() {
    claims[ISSUER_ENTRY] = ISSUER
    val jwt = Jwt(JWT_VALUE, Instant.now(), Instant.now().plusSeconds(120), headers, claims)

    testUser.admin = false
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
    claims.remove(SUBJECT_ENTRY)
    val jwt = Jwt(JWT_VALUE, Instant.now(), Instant.now().plusSeconds(120), headers, claims)

    assertThatThrownBy { cut.convert(jwt) }.isInstanceOf(OAuth2AuthenticationException::class.java)
  }

  companion object {
    private const val EMAIL = "hans.mustermann@example.com"
    private const val USERID_USER = EMAIL
    private const val USERID_UNKNOWN = "unknown"
    private const val FIRST_NAME = "Hans"
    private const val LAST_NAME = "Mustermann"
    private const val ISSUER = "https://example.com"
    private const val SUBJECT_ENTRY = "sub"
    private const val ISSUER_ENTRY = "iss"
    private const val EMAIL_ENTRY = "email"
    private const val JWT_VALUE = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dC..."
  }
}
