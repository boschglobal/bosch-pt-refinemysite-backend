/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common.facade.rest

import com.bosch.pt.csm.application.SmartSiteMockKTest
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.apache.commons.lang3.StringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders.IF_MATCH
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.ModelAndViewContainer

/** Unit test to verify correctly resolving the 'IF-MATCH' header. */
@SmartSiteMockKTest
class ETagArgumentResolverTest {

  private val cut = ETagArgumentResolver()

  @MockK private lateinit var methodParameter: MethodParameter

  @MockK private lateinit var modelAndViewContainer: ModelAndViewContainer

  @MockK private lateinit var webRequest: NativeWebRequest

  @MockK private lateinit var webDataBinderFactory: WebDataBinderFactory

  /** Verify that resolver supports parameters of type [ETag]. */
  @Test
  fun verifySupportsParameter() {
    every { methodParameter.parameterType } returns ETag::class.java
    cut.supportsParameter(methodParameter).apply { assertThat(this).isTrue }
  }

  /** Verify that resolver does not support any other parameter type than [ETag]. */
  @Test
  fun verifyNotSupportsParameter() {
    every { methodParameter.parameterType } returns String::class.java
    cut.supportsParameter(methodParameter).apply { assertThat(this).isFalse }
  }

  /** Verify that resolver resolves 'IF-MATCH' http header correctly to [ETag] instance. */
  @Test
  fun verifyResolveArgumentForIfMatchHeader() {
    every { webRequest.getHeader(IF_MATCH) } returns StringUtils.wrap("1", '"')
    cut.resolveArgument(methodParameter, modelAndViewContainer, webRequest, webDataBinderFactory)
        .apply { assertThat(this.toString()).isNotNull.isEqualTo(StringUtils.wrap("1", '"')) }
  }

  /** Verify that resolver reports an error when no 'IF-MATCH' header is given. */
  @Test
  fun verifyResolveArgumentForMissingIfMatchHeader() {
    every { webRequest.getHeader(IF_MATCH) } returns null
    assertThatThrownBy {
          cut.resolveArgument(
              methodParameter, modelAndViewContainer, webRequest, webDataBinderFactory)
        }
        .isInstanceOf(HttpMessageNotReadableException::class.java)
  }

  /** Verify that resolver reports an error when empty 'IF-MATCH' header value is given. */
  @Test
  fun verifyResolveArgumentForEmptyIfMatchHeader() {
    every { webRequest.getHeader(IF_MATCH) } returns StringUtils.EMPTY
    assertThatThrownBy {
          cut.resolveArgument(
              methodParameter, modelAndViewContainer, webRequest, webDataBinderFactory)
        }
        .isInstanceOf(HttpMessageNotReadableException::class.java)
        .hasMessage("Value for request header field 'If-Match' is missing.")
  }
}
