/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.businesstransaction.facade.rest

import com.bosch.pt.iot.smartsite.common.businesstransaction.BusinessTransactionContextHolder
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.web.filter.OncePerRequestFilter

/**
 * This filter ensures that no business transaction is open before the thread associated with this
 * request is released back to its thread pool. Otherwise, the same business transaction might span
 * multiple requests from different users. The reason is that a ThreadLocal is used for managing the
 * business transaction identifier.
 */
class BusinessTransactionCleanupFilter : OncePerRequestFilter() {

  override fun doFilterInternal(
      request: HttpServletRequest,
      response: HttpServletResponse,
      filterChain: FilterChain
  ) {
    filterChain.doFilter(request, response)
    ensureBusinessTransactionClosed()
  }

  private fun ensureBusinessTransactionClosed() {
    // needs to be a loop because there could be inner business transactions that need to be closed
    // from the inside out until eventually the outermost transaction can be closed.
    while (BusinessTransactionContextHolder.isBusinessTransactionActive()) {
      BusinessTransactionContextHolder.closeContext()
      LOGGER.warn(
          "Detected open business transaction after request handling. This seems to be a bug. Always make sure " +
              "business trasnactions are closed properly!")
    }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(BusinessTransactionCleanupFilter::class.java)
  }
}
