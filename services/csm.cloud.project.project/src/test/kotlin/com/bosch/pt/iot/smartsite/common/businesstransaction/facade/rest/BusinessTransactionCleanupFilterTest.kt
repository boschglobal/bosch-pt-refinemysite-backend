/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.businesstransaction.facade.rest

import com.bosch.pt.iot.smartsite.common.businesstransaction.BusinessTransactionContextHolder
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class BusinessTransactionCleanupFilterTest {

  val cut: BusinessTransactionCleanupFilter = BusinessTransactionCleanupFilter()

  @Test
  fun `applying filter closes active business transactions`() {
    BusinessTransactionContextHolder.openContext()
    BusinessTransactionContextHolder.openContext()

    cut.doFilter(mockRequest(), mockk<HttpServletResponse>(), mockk(relaxed = true))

    assertThat(BusinessTransactionContextHolder.isBusinessTransactionActive()).isFalse
  }

  @Test
  fun `applying filter when no transaction is active succeeds`() {
    cut.doFilter(mockRequest(), mockk<HttpServletResponse>(), mockk(relaxed = true))

    assertThat(BusinessTransactionContextHolder.isBusinessTransactionActive()).isFalse
  }

  private fun mockRequest(): HttpServletRequest {
    val request = mockk<HttpServletRequest>(relaxed = true)

    // this is required so that doFilterInternal() is actually called on the filter
    every { request.getAttribute(any()) } returns null
    return request
  }
}
