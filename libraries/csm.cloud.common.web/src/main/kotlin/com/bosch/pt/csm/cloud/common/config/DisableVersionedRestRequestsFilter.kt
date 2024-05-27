/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.config

import com.bosch.pt.csm.cloud.common.exceptions.BlockOperationsException
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.extractApiVersion
import com.bosch.pt.csm.cloud.common.i18n.CommonKey.BLOCK_ALL_OPERATIONS
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

class DisableVersionedRestRequestsFilter : OncePerRequestFilter() {

  override fun doFilterInternal(
      request: HttpServletRequest,
      response: HttpServletResponse,
      filterChain: FilterChain
  ) {
    if (extractApiVersion(request.requestURI) > 0) {
      throw BlockOperationsException(BLOCK_ALL_OPERATIONS)
    }
    filterChain.doFilter(request, response)
  }
}
