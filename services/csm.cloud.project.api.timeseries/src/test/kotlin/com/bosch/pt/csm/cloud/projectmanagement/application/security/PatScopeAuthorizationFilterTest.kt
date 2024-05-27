/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.details.PatUserDetailsAuthenticationToken
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authorization.PatScopeAuthorizationFilter
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authorization.InsufficientPatScopeException
import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.rest.ProjectRestController.Companion.PROJECTS_ENDPOINT
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.domain.asPatId
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatProjection
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatScopeEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatScopeEnum.GRAPHQL_API_READ
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatScopeEnum.TIMELINE_API_READ
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatTypeEnum.RMSPAT1
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.model.UserProjection
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import java.time.LocalDateTime
import java.util.Locale.GERMANY
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.context.TestSecurityContextHolder

@SmartSiteMockKTest
class PatScopeAuthorizationFilterTest {

  private val cut = PatScopeAuthorizationFilter()

  @BeforeEach
  fun init() {
    TestSecurityContextHolder.clearContext()
  }

  @AfterEach
  fun clear() {
    TestSecurityContextHolder.clearContext()
  }

  @ValueSource(strings = ["", "/timeline"])
  @ParameterizedTest
  fun `timeline scope access allowed`(prefix: String) {
    val request = MockHttpServletRequest().apply { requestURI = "$prefix/v1/$PROJECTS_ENDPOINT" }
    val response = MockHttpServletResponse()
    val filterChain = mockk<FilterChain>(relaxed = true)

    val pat = patProjection(listOf(TIMELINE_API_READ))
    TestSecurityContextHolder.getContext().authentication =
        PatUserDetailsAuthenticationToken(pat, pat.authorities)

    cut.doFilter(request, response, filterChain)

    verify(exactly = 1) { filterChain.doFilter(request, response) }
  }

  @ValueSource(strings = ["", "/timeline"])
  @ParameterizedTest
  fun `missing timeline scope permission`(prefix: String) {
    val request = MockHttpServletRequest().apply { requestURI = "$prefix/v1/$PROJECTS_ENDPOINT" }
    val response = MockHttpServletResponse()
    val filterChain = mockk<FilterChain>(relaxed = true)

    val pat = patProjection(listOf(GRAPHQL_API_READ))
    TestSecurityContextHolder.getContext().authentication =
        PatUserDetailsAuthenticationToken(pat, pat.authorities)

    assertThatExceptionOfType(InsufficientPatScopeException::class.java)
        .isThrownBy { cut.doFilter(request, response, filterChain) }
        .withMessage("Access to Timeline API not granted")

    verify(exactly = 0) { filterChain.doFilter(any(), any()) }
  }

  @Test
  fun `graphql scope access allowed`() {
    val request = MockHttpServletRequest().apply { requestURI = "/graphql" }
    val response = MockHttpServletResponse()
    val filterChain = mockk<FilterChain>(relaxed = true)

    val pat = patProjection(listOf(GRAPHQL_API_READ))
    TestSecurityContextHolder.getContext().authentication =
        PatUserDetailsAuthenticationToken(pat, pat.authorities)

    cut.doFilter(request, response, filterChain)

    verify(exactly = 1) { filterChain.doFilter(request, response) }
  }

  @Test
  fun `missing graphql scope permission`() {
    val request = MockHttpServletRequest().apply { requestURI = "/graphql" }
    val response = MockHttpServletResponse()
    val filterChain = mockk<FilterChain>(relaxed = true)

    val pat = patProjection(listOf(TIMELINE_API_READ))
    TestSecurityContextHolder.getContext().authentication =
        PatUserDetailsAuthenticationToken(pat, pat.authorities)

    assertThatExceptionOfType(InsufficientPatScopeException::class.java)
        .isThrownBy { cut.doFilter(request, response, filterChain) }
        .withMessage("Access to GraphQL API not granted")

    verify(exactly = 0) { filterChain.doFilter(any(), any()) }
  }

  @Test
  fun `ignore non graphql and timeline api calls`() {
    val request = MockHttpServletRequest().apply { requestURI = "/other" }
    val response = MockHttpServletResponse()
    val filterChain = mockk<FilterChain>(relaxed = true)

    val pat = patProjection(listOf())
    TestSecurityContextHolder.getContext().authentication =
        PatUserDetailsAuthenticationToken(pat, pat.authorities)

    cut.doFilter(request, response, filterChain)

    verify(exactly = 1) { filterChain.doFilter(any(), any()) }
  }

  @Test
  fun `ignore if authentication is no PAT`() {
    val request = MockHttpServletRequest().apply { requestURI = "/other" }
    val response = MockHttpServletResponse()
    val filterChain = mockk<FilterChain>(relaxed = true)

    val userProjection = userProjection()
    TestSecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(userProjection, "password", userProjection.authorities)

    cut.doFilter(request, response, filterChain)

    verify(exactly = 1) { filterChain.doFilter(any(), any()) }
  }

  @Test
  fun `fail if no authentication is set`() {
    val request = MockHttpServletRequest().apply { requestURI = "/other" }
    val response = MockHttpServletResponse()
    val filterChain = mockk<FilterChain>(relaxed = true)

    assertThatExceptionOfType(InsufficientAuthenticationException::class.java)
        .isThrownBy { cut.doFilter(request, response, filterChain) }
        .withMessage("Fully authenticated user is required")

    verify(exactly = 0) { filterChain.doFilter(any(), any()) }
  }

  private fun patProjection(scopes: List<PatScopeEnum>) =
      PatProjection(
          randomUUID().asPatId(),
          0L,
          "description",
          randomUUID().asUserId(),
          "\$2a\$10\$.alDG9I8Q2l1YR265lFxDeJ88iTecGPCRSSOA9b4HHKFfPk1k/zcC",
          RMSPAT1,
          scopes,
          LocalDateTime.now(),
          LocalDateTime.now().plusMinutes(5),
          false,
          LocalDateTime.now(),
          GERMANY)

  private fun userProjection() =
      UserProjection(
          randomUUID().asUserId(),
          0L,
          "idp",
          "a",
          "a",
          "a",
          null,
          null,
          admin = false,
          locked = false,
          locale = GERMANY,
          country = null,
          crafts = emptyList(),
          phoneNumbers = emptyList(),
          eventAuthor = randomUUID().asUserId(),
          eventDate = LocalDateTime.now(),
          history = emptyList())
}
