/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.common.security.CustomTrustedJwtIssuersProperties
import com.bosch.pt.csm.cloud.common.security.DefaultCustomUserAuthenticationConverter
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.User
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import java.util.Locale
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.jwt.Jwt

@ExtendWith(MockKExtension::class)
internal class CustomUserAuthenticationConverterTest {

  @MockK private lateinit var userDetailsService: UserDetailsService

  private lateinit var cut: DefaultCustomUserAuthenticationConverter

  @BeforeEach
  fun init() {
    cut =
        DefaultCustomUserAuthenticationConverter(
            userDetailsService, emptyList(), CustomTrustedJwtIssuersProperties(listOf(ISSUER)))
  }

  @Test
  fun verifyConvertFailedInvalidIssuer() {
    val issuer = randomUUID().toString() as Any
    val claims = mapOf("iss" to issuer)
    val headers = mapOf("fakeHeader" to randomUUID() as Any)
    val jwt = Jwt(randomUUID().toString(), null, null, headers, claims)

    assertThatExceptionOfType(OAuth2AuthenticationException::class.java)
        .isThrownBy { cut.convert(jwt) }
        .usingComparator(
            Comparator.comparing { ex: OAuth2AuthenticationException -> ex.error.errorCode })
        .isEqualTo(OAuth2AuthenticationException(OAuth2Error("Untrusted issuer $issuer")))
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
  }

  @Test
  fun verifyConvertUser() {
    val claims = HashMap<String, Any>()
    claims["iss"] = ISSUER
    claims["sub"] = randomUUID().toString()

    val headers = mapOf("fakeHeader" to randomUUID() as Any)
    val jwt = Jwt(randomUUID().toString(), null, null, headers, claims)

    val user =
        User(
            randomUUID().toString(),
            randomUUID(),
            admin = false,
            locked = false,
            locale = Locale.UK)

    every { userDetailsService.loadUserByUsername(any()) } returns user

    val authentication = cut.convert(jwt)
    assertThat(authentication).isNotNull
    assertThat(authentication.principal).isEqualTo(user)
    assertThat(authentication.authorities)
        .containsAll(AuthorityUtils.createAuthorityList(RoleConstants.USER.roleName()))
  }

  @Test
  fun verifyConvertAdmin() {
    val claims = HashMap<String, Any>()
    claims["iss"] = ISSUER
    claims["sub"] = randomUUID().toString()

    val headers = mapOf("fakeHeader" to randomUUID() as Any)
    val jwt = Jwt(randomUUID().toString(), null, null, headers, claims)

    val user =
        User(
            randomUUID().toString(), randomUUID(), admin = true, locked = false, locale = Locale.UK)

    every { userDetailsService.loadUserByUsername(any()) } returns user

    val authentication = cut.convert(jwt)
    assertThat(authentication).isNotNull
    assertThat(authentication.principal).isEqualTo(user)
    assertThat(authentication.authorities)
        .containsAll(
            AuthorityUtils.createAuthorityList(
                RoleConstants.USER.roleName(), RoleConstants.ADMIN.roleName()))
  }

  companion object {
    private const val ISSUER = "test"
  }
}
