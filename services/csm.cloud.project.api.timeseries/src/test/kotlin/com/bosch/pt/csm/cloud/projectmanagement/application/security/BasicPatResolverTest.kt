/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.resolver.BasicPatResolver
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.PatAuthenticationException
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationConverter
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter

@SmartSiteMockKTest
class BasicPatResolverTest {

  @RelaxedMockK private lateinit var authenticationConverter: AuthenticationConverter

  @RelaxedMockK private lateinit var authentication: Authentication

  @RelaxedMockK private lateinit var request: HttpServletRequest

  private val token = "RMSPAT1.7281b943e25b4822bd92c330ebcc9a59.xbmb7t9pQ8z8veA2MlwlOXvBxMvDDcpd"

  private val basicToken =
      "OlJNU1BBVDEuNzI4MWI5NDNlMjViNDgyMmJkOTJjMzMwZWJjYzlhNTkueGJtYjd0OXBROHo4dmVBMk1sd2xPWHZCeE12RERjcGQ="

  private val cut: BasicPatResolver by lazy {
    BasicPatResolver().apply { setAuthenticationConverter(authenticationConverter) }
  }

  @Test
  fun `resolve pat`() {
    val token = "abcABC019."

    every { authenticationConverter.convert(any()) } returns authentication
    every { authentication.credentials } returns token

    assertThat(cut.resolve(request)).isEqualTo(token)
  }

  @Test
  fun `resolve null if token is resolved to null`() {
    every { authenticationConverter.convert(any()) } returns null

    assertThat(cut.resolve(request)).isNull()
  }

  @Test
  fun `resolve null if credentials are null`() {
    every { authenticationConverter.convert(any()) } returns authentication
    every { authentication.credentials } returns null

    assertThat(cut.resolve(request)).isNull()
  }

  @Test
  fun `error if conversion of pat fails`() {
    every { authenticationConverter.convert(any()) } throws BadCredentialsException("error")

    assertThatExceptionOfType(PatAuthenticationException::class.java)
        .isThrownBy { cut.resolve(request) }
        .withMessage("Invalid basic authentication token")
        .withCauseInstanceOf(BadCredentialsException::class.java)
  }

  @Test
  fun `error if pat contains invalid characters`() {
    val token = "abc$$**()."

    every { authenticationConverter.convert(any()) } returns authentication
    every { authentication.credentials } returns token

    assertThatExceptionOfType(PatAuthenticationException::class.java)
        .isThrownBy { cut.resolve(request) }
        .withMessage("Personal access token is malformed")
        .withNoCause()
  }

  @Test
  fun `test basic scheme lowercase`() {
    val tokenResolver = initActualResolver()

    val request = MockHttpServletRequest().apply { addHeader(AUTHORIZATION, "basic $basicToken") }
    assertThat(tokenResolver.resolve(request)).isEqualTo(token)
  }

  @Test
  fun `test basic scheme uppercase`() {
    val tokenResolver = initActualResolver()

    val request = MockHttpServletRequest().apply { addHeader(AUTHORIZATION, "BASIC $basicToken") }
    assertThat(tokenResolver.resolve(request)).isEqualTo(token)
  }

  private fun initActualResolver(): BasicPatResolver {
    val detailSource = WebAuthenticationDetailsSource()
    val basicAuthenticationConverter = BasicAuthenticationConverter(detailSource)
    return BasicPatResolver().apply { setAuthenticationConverter(basicAuthenticationConverter) }
  }
}
