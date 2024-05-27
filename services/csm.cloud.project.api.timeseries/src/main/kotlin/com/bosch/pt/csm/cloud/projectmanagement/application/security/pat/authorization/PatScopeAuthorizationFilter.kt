/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authorization

import com.bosch.pt.csm.cloud.projectmanagement.application.security.SecurityContextHelper
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatProjection
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatScopeEnum.GRAPHQL_API_READ
import com.bosch.pt.csm.cloud.projectmanagement.user.pat.query.model.PatScopeEnum.TIMELINE_API_READ
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

class PatScopeAuthorizationFilter : OncePerRequestFilter() {
  override fun doFilterInternal(
      request: HttpServletRequest,
      response: HttpServletResponse,
      filterChain: FilterChain
  ) {
    val userDetails = SecurityContextHelper.getCurrentUserDetails()
    if (userDetails is PatProjection) {
      if (request.requestURI.startsWith("/graphql") &&
          !userDetails.scopes.contains(GRAPHQL_API_READ)) {
        throw InsufficientPatScopeException("Access to GraphQL API not granted")
      } else if (request.requestURI.contains(TIMELINE_API_REGEX) &&
          !userDetails.scopes.contains(TIMELINE_API_READ)) {
        throw InsufficientPatScopeException("Access to Timeline API not granted")
      }
    }
    filterChain.doFilter(request, response)
  }

  companion object {
    // The /timeline prefix is not available in the tests as the controllers in the
    // service do not map the /timeline prefix
    val TIMELINE_API_REGEX = Regex(".*(/timeline)?/v[0-9]/.*")
  }
}
