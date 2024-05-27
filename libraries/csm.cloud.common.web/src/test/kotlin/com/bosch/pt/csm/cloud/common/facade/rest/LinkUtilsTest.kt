/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersionPrefix
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@ExtendWith(MockKExtension::class)
class LinkUtilsTest {

  @MockK private lateinit var requestAttributes: ServletRequestAttributes

  @MockK private lateinit var servletRequest: HttpServletRequest

  @BeforeEach
  fun init() {
    every { requestAttributes.request }.returns(servletRequest)
    RequestContextHolder.setRequestAttributes(requestAttributes)
  }

  @AfterEach
  fun clean() {
    RequestContextHolder.resetRequestAttributes()
  }

  @Test
  fun verifyGetCurrentApiVersion() {
    every { servletRequest.requestURI }.returns("/v1/test")
    assertThat(getCurrentApiVersion()).isEqualTo(1)
  }

  @Test
  fun verifyGetCurrentApiVersionFailsWithoutVersion() {
    every { servletRequest.requestURI }.returns("/test")
    assertThatExceptionOfType(IllegalStateException::class.java)
        .isThrownBy { getCurrentApiVersion() }
        .withMessage("No version information found in url path")
  }

  @Test
  fun verifyGetCurrentApiVersionPrefix() {
    every { servletRequest.requestURI }.returns("/v1/test")
    assertThat(getCurrentApiVersionPrefix()).isEqualTo("/v1")
  }
}
