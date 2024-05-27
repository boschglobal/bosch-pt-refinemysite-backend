/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.PatAuthenticationEntryPoint
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.PatAuthenticationFilter
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.resolver.TokenResolver
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.PatAuthenticationException
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.model.PatErrors
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.AuthenticationDetailsSource
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationManagerResolver
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolderStrategy
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.context.SecurityContextRepository

@SmartSiteMockKTest
class PatAuthenticationFilterTest {

  @RelaxedMockK
  private lateinit var authenticationManagerResolver:
      AuthenticationManagerResolver<HttpServletRequest>

  @RelaxedMockK private lateinit var securityContextHolderStrategy: SecurityContextHolderStrategy

  @RelaxedMockK private lateinit var tokenResolver: TokenResolver

  @RelaxedMockK private lateinit var entryPoint: PatAuthenticationEntryPoint

  @RelaxedMockK private lateinit var securityContextRepository: SecurityContextRepository

  @RelaxedMockK
  private lateinit var authenticationDetailsSource:
      AuthenticationDetailsSource<HttpServletRequest, *>

  private val cut by lazy {
    PatAuthenticationFilter(
        authenticationManagerResolver,
        securityContextHolderStrategy,
        tokenResolver,
        entryPoint,
        securityContextRepository,
        authenticationDetailsSource)
  }

  @Test
  fun `handle error and do not process the request if the token cannot be resolved`() {
    val request = MockHttpServletRequest()
    val response = MockHttpServletResponse()
    val filterChain = mockk<FilterChain>()

    val expectedException =
        PatAuthenticationException(PatErrors.invalidToken("Invalid basic authentication token"))
    every { tokenResolver.resolve(any()) } throws expectedException

    cut.doFilter(request, response, filterChain)

    verify(exactly = 1) { entryPoint.commence(request, response, expectedException) }
    verify(exactly = 0) { filterChain.doFilter(any(), any()) }
  }

  @Test
  fun `do not apply the filter if no pat is provided`() {
    val request = MockHttpServletRequest()
    val response = MockHttpServletResponse()
    val filterChain = mockk<FilterChain>(relaxed = true)

    every { tokenResolver.resolve(any()) } returns null

    cut.doFilter(request, response, filterChain)

    verify(exactly = 1) { filterChain.doFilter(any(), any()) }
    verify(exactly = 0) { entryPoint.commence(any(), any(), any()) }
  }

  @Test
  fun `authentication failed because user is locked`() {
    val request = MockHttpServletRequest()
    val response = MockHttpServletResponse()
    val filterChain = mockk<FilterChain>()
    val authenticationManager = mockk<AuthenticationManager>()

    every { tokenResolver.resolve(any()) } returns "token"
    every { authenticationManagerResolver.resolve(any()) } returns authenticationManager
    val expectedException = LockedException("Locked")
    every { authenticationManager.authenticate(any()) } throws expectedException

    cut.doFilter(request, response, filterChain)

    verifySequence {
      tokenResolver.resolve(request)
      authenticationDetailsSource.buildDetails(request)
      authenticationManagerResolver.resolve(request)
      authenticationManager.authenticate(any())
      securityContextHolderStrategy.clearContext()
      entryPoint.commence(request, response, expectedException)
    }

    verify(exactly = 0) { filterChain.doFilter(any(), any()) }
  }

  @Test
  fun `successful authentication`() {
    val request = MockHttpServletRequest()
    val response = MockHttpServletResponse()
    val filterChain = mockk<FilterChain>(relaxed = true)
    val authenticationManager = mockk<AuthenticationManager>()
    val authentication = mockk<Authentication>()
    val securityContext = SecurityContextImpl()

    every { tokenResolver.resolve(any()) } returns "token"
    every { authenticationManagerResolver.resolve(any()) } returns authenticationManager
    every { authenticationManager.authenticate(any()) } returns authentication
    every { securityContextHolderStrategy.createEmptyContext() } returns securityContext

    cut.doFilter(request, response, filterChain)

    verifySequence {
      tokenResolver.resolve(request)
      authenticationDetailsSource.buildDetails(request)
      authenticationManagerResolver.resolve(request)
      authenticationManager.authenticate(any())
      securityContextHolderStrategy.createEmptyContext()
      securityContextHolderStrategy.context = securityContext
      securityContextRepository.saveContext(securityContext, request, response)
      filterChain.doFilter(any(), any())
    }

    verify(exactly = 0) { entryPoint.commence(any(), any(), any()) }

    assertThat(securityContext.authentication).isEqualTo(authentication)
  }
}
