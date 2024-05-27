/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_INVALID_TOKEN
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_NO_TOKEN_PROVIDED
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_USER_LOCKED
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.SERVER_ERROR_USER_NOT_REGISTERED
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model.PatErrorCodes.INVALID_TOKEN
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.PatAuthenticationEntryPoint
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.PatAuthenticationError
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.PatAuthenticationException
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model.PatErrors
import com.bosch.pt.csm.cloud.projectmanagement.common.translation.Key.PAT_EXPIRED
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.MessageSource
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders.WWW_AUTHENTICATE
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.userdetails.UsernameNotFoundException

@SmartSiteMockKTest
class PatAuthenticationEntryPointTest {

  @RelaxedMockK private lateinit var messageSource: MessageSource

  @RelaxedMockK private lateinit var environment: Environment

  private val cut by lazy { PatAuthenticationEntryPoint(messageSource, environment) }

  @Test
  fun `handle UsernameNotFoundException`() {
    val request = MockHttpServletRequest()
    val response = MockHttpServletResponse()

    every { messageSource.getMessage(SERVER_ERROR_USER_NOT_REGISTERED, any(), any()) } returns
        SERVER_ERROR_USER_NOT_REGISTERED

    cut.commence(request, response, UsernameNotFoundException("Not found"))

    assertThat(response.status).isEqualTo(FORBIDDEN.value())
    assertThat(response.contentAsString)
        .isEqualTo("{\"message\":\"ServerError_UserNotRegistered\",\"traceId\":\"0\"}")
  }

  @Test
  fun `handle LockedException`() {
    val request = MockHttpServletRequest()
    val response = MockHttpServletResponse()

    every { messageSource.getMessage(SERVER_ERROR_USER_LOCKED, any(), any()) } returns
        SERVER_ERROR_USER_LOCKED

    cut.commence(request, response, LockedException("Locked"))

    assertThat(response.status).isEqualTo(FORBIDDEN.value())
    assertThat(response.contentAsString)
        .isEqualTo("{\"message\":\"ServerError_UserLocked\",\"traceId\":\"0\"}")
  }

  @Test
  fun `handle CredentialsExpiredException`() {
    val request = MockHttpServletRequest()
    val response = MockHttpServletResponse()

    every { messageSource.getMessage(PAT_EXPIRED, any(), any()) } returns PAT_EXPIRED

    cut.commence(request, response, CredentialsExpiredException("Pat expired"))

    assertThat(response.status).isEqualTo(FORBIDDEN.value())
    assertThat(response.contentAsString).isEqualTo("{\"message\":\"PatExpired\",\"traceId\":\"0\"}")
  }

  @Test
  fun `handle PatAuthenticationException`() {
    val request = MockHttpServletRequest()
    val response = MockHttpServletResponse()

    every { messageSource.getMessage(SERVER_ERROR_NO_TOKEN_PROVIDED, any(), any()) } returns
        SERVER_ERROR_NO_TOKEN_PROVIDED

    cut.commence(
        request,
        response,
        PatAuthenticationException(PatAuthenticationError(INVALID_TOKEN, "description", "uri"))
    )

    assertThat(response.status).isEqualTo(UNAUTHORIZED.value())
    assertThat(response.contentAsString)
        .isEqualTo("{\"message\":\"ServerError_NoTokenProvided\",\"traceId\":\"0\"}")
    assertThat(response.getHeader(WWW_AUTHENTICATE))
        .isEqualTo(
            "PAT error=\"invalid_token\", error_description=\"description\", error_uri=\"uri\"")
  }

  @Test
  fun `handle PatAuthenticationException with PatTokenError`() {
    val request = MockHttpServletRequest()
    val response = MockHttpServletResponse()

    every { messageSource.getMessage(SERVER_ERROR_INVALID_TOKEN, any(), any()) } returns
        SERVER_ERROR_INVALID_TOKEN

    cut.commence(
        request, response, PatAuthenticationException(PatErrors.invalidToken("description"))
    )

    assertThat(response.status).isEqualTo(UNAUTHORIZED.value())
    assertThat(response.contentAsString)
        .isEqualTo("{\"message\":\"ServerError_InvalidToken\",\"traceId\":\"0\"}")
    assertThat(response.getHeader(WWW_AUTHENTICATE))
        .isEqualTo(
            "PAT error=\"invalid_token\", error_description=\"description\", " +
                "error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\"")
  }

  @Test
  fun `handle AuthenticationException`() {
    val request = MockHttpServletRequest()
    val response = MockHttpServletResponse()
    cut.commence(request, response, BadCredentialsException("Access denied"))

    assertThat(response.status).isEqualTo(UNAUTHORIZED.value())
    assertThat(response.getHeader(WWW_AUTHENTICATE)).isEqualTo("Unauthorized")
  }
}
