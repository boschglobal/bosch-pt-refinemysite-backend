/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.common.security.CustomTrustedJwtIssuersProperties
import com.bosch.pt.csm.cloud.common.security.DefaultCustomUserAuthenticationConverter
import com.bosch.pt.csm.cloud.projectmanagement.test.randomString
import com.bosch.pt.csm.cloud.projectmanagement.test.randomUUID
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.AuthorityUtils.createAuthorityList
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.jwt.Jwt

@DisplayName("Verify correct conversion of authentication")
class CustomUserAuthenticationConverterTest {

  private val userDetailsService = mockk<UserDetailsServiceImpl>(relaxed = true)
  private lateinit var cut: DefaultCustomUserAuthenticationConverter

  @BeforeEach
  fun init() {
    cut =
        DefaultCustomUserAuthenticationConverter(
            userDetailsService, emptyList(), CustomTrustedJwtIssuersProperties(listOf(ISSUER)))
  }

  @Test
  fun verifyConvertFailedInvalidIssuer() {
    val issuer = randomString() as Any
    val claims = mapOf("iss" to issuer)
    val headers = mapOf("fakeHeader" to randomUUID() as Any)
    val jwt = Jwt(randomString(), null, null, headers, claims)

    assertThatExceptionOfType(OAuth2AuthenticationException::class.java)
        .isThrownBy { cut.convert(jwt) }
        .usingComparator(
            Comparator.comparing<OAuth2AuthenticationException, String> { it.error.errorCode })
        .isEqualTo(OAuth2AuthenticationException(OAuth2Error("Untrusted issuer $issuer")))
  }

  @Test
  fun verifyConvertFailedUsernameNotFound() {
    val ciamId = randomString()
    val claims = mapOf("iss" to ISSUER, "sub" to ciamId)
    val headers = mapOf("fakeHeader" to randomUUID() as Any)
    val jwt = Jwt(randomString(), null, null, headers, claims)
    every { userDetailsService.loadUserByUsername(ciamId) } throws
        UsernameNotFoundException("not found")

    assertThatExceptionOfType(UsernameNotFoundException::class.java).isThrownBy { cut.convert(jwt) }
  }

  @Test
  fun verifyConvertAdminNull() {
    val ciamId = randomString()
    val claims = mapOf("iss" to ISSUER, "sub" to ciamId)
    val headers = mapOf("fakeHeader" to randomUUID() as Any)
    val jwt = Jwt(randomString(), null, null, headers, claims)
    val user =
        User(
            identifier = randomUUID(),
            displayName = randomString(),
            externalIdentifier = ciamId,
            admin = null)
    every { userDetailsService.loadUserByUsername(ciamId) } returns user

    val authentication = cut.convert(jwt)

    assertThat(authentication).isNotNull
    assertThat(authentication.principal).isEqualTo(user)
    assertThat(authentication.authorities)
        .containsAll(createAuthorityList(RoleConstants.USER.roleName()))
  }

  @Test
  fun verifyConvertUser() {
    val ciamId = randomString()
    val claims = mapOf("iss" to ISSUER, "sub" to ciamId)
    val headers = mapOf("fakeHeader" to randomUUID() as Any)
    val jwt = Jwt(randomString(), null, null, headers, claims)
    val user =
        User(
            identifier = randomUUID(),
            displayName = randomString(),
            externalIdentifier = ciamId,
            admin = false)
    every { userDetailsService.loadUserByUsername(ciamId) } returns user

    val authentication = cut.convert(jwt)

    assertThat(authentication).isNotNull
    assertThat(authentication.principal).isEqualTo(user)
    assertThat(authentication.authorities)
        .containsAll(createAuthorityList(RoleConstants.USER.roleName()))
  }

  @Test
  fun verifyConvertAdmin() {
    val ciamId = randomString()
    val claims = mapOf("iss" to ISSUER, "sub" to ciamId)
    val headers = mapOf("fakeHeader" to randomUUID() as Any)
    val jwt = Jwt(randomString(), null, null, headers, claims)
    val user = User(identifier = randomUUID(), displayName = randomString(), admin = true)
    every { userDetailsService.loadUserByUsername(ciamId) } returns user

    val authentication = cut.convert(jwt)

    assertThat(authentication).isNotNull
    assertThat(authentication.principal).isEqualTo(user)
    assertThat(authentication.authorities)
        .containsAll(
            createAuthorityList(RoleConstants.USER.roleName(), RoleConstants.ADMIN.roleName()))
  }

  companion object {

    private const val ISSUER = "test"
  }
}
