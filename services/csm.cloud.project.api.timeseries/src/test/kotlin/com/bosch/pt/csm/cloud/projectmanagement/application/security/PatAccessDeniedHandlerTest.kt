/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authorization.InsufficientPatScopeException
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.handler.PatAccessDeniedHandler
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.WWW_AUTHENTICATE
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException

@SmartSiteMockKTest
class PatAccessDeniedHandlerTest {

  @RelaxedMockK private lateinit var request: HttpServletRequest

  private var response: HttpServletResponse = MockHttpServletResponse()

  private val cut = PatAccessDeniedHandler()

  @Test
  fun `handle InsufficientPatScopeException with message`() {
    val errorMessage = "Access to GraphQL API not granted"
    cut.handle(request, response, InsufficientPatScopeException(errorMessage))

    assertThat(response.status).isEqualTo(FORBIDDEN.value())
    assertThat(response.getHeader(WWW_AUTHENTICATE))
        .isEqualTo("PAT error=\"insufficient_scope\", error_description=\"$errorMessage\"")
  }

  @Test
  fun `handle InsufficientPatScopeException without message`() {
    val errorMessage = "The request requires higher privileges than provided by the access token."
    val invalidExceptionWithoutMessage = mockk<InsufficientPatScopeException>(relaxed = true)
    every { invalidExceptionWithoutMessage.message } returns null
    cut.handle(request, response, invalidExceptionWithoutMessage)

    assertThat(response.status).isEqualTo(FORBIDDEN.value())
    assertThat(response.getHeader(WWW_AUTHENTICATE))
        .isEqualTo("PAT error=\"insufficient_scope\", error_description=\"$errorMessage\"")
  }

  @Test
  fun `handle AccessDeniedException`() {
    val accessDeniedException = AccessDeniedException("Access denied")
    cut.handle(request, response, accessDeniedException)

    assertThat(response.status).isEqualTo(FORBIDDEN.value())
    assertThat(response.getHeader(WWW_AUTHENTICATE)).isEqualTo("PAT is invalid")
  }
}
