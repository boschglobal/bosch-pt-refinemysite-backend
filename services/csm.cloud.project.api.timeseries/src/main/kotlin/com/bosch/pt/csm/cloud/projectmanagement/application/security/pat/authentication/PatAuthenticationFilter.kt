/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication

import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.PatAuthenticationToken
import com.bosch.pt.csm.cloud.projectmanagement.application.security.pat.authentication.token.resolver.TokenResolver
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.IOException
import org.springframework.security.authentication.AuthenticationDetailsSource
import org.springframework.security.authentication.AuthenticationManagerResolver
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolderStrategy
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Translation of
 * [org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter] for
 * PATs.
 */
class PatAuthenticationFilter(
    private val authenticationManagerResolver: AuthenticationManagerResolver<HttpServletRequest>,
    private val securityContextHolderStrategy: SecurityContextHolderStrategy,
    private val tokenResolver: TokenResolver,
    private val entryPoint: PatAuthenticationEntryPoint,
    private val securityContextRepository: SecurityContextRepository,
    private val authenticationDetailsSource: AuthenticationDetailsSource<HttpServletRequest, *>
) : OncePerRequestFilter() {

  private val authenticationFailureHandler =
      AuthenticationFailureHandler {
          request: HttpServletRequest,
          response: HttpServletResponse,
          exception: AuthenticationException ->
        // The following check is kept as in oauth implementation to handle issues in the
        // authentication implementation code
        if (exception is AuthenticationServiceException) {
          throw exception
        }
        this.entryPoint.commence(request, response, exception)
      }

  @Throws(ServletException::class, IOException::class)
  override fun doFilterInternal(
      request: HttpServletRequest,
      response: HttpServletResponse,
      filterChain: FilterChain
  ) {
    val token: String? =
        try {
          this.tokenResolver.resolve(request)
        } catch (invalid: AuthenticationException) {
          logger.trace(
              "Sending to authentication entry point since failed to resolve pat token", invalid)
          this.entryPoint.commence(request, response, invalid)
          return
        }

    if (token == null) {
      logger.trace("Did not process request since did not find pat token")
      filterChain.doFilter(request, response)
      return
    }

    val authenticationRequest =
        PatAuthenticationToken(token).apply {
          details = authenticationDetailsSource.buildDetails(request)
        }

    try {
      val authenticationManager = authenticationManagerResolver.resolve(request)
      val authenticationResult = authenticationManager.authenticate(authenticationRequest)

      securityContextHolderStrategy
          .createEmptyContext()
          .apply { authentication = authenticationResult }
          .also { context ->
            securityContextHolderStrategy.context = context
            securityContextRepository.saveContext(context, request, response)
          }

      if (logger.isDebugEnabled) {
        logger.debug("Set SecurityContextHolder to $authenticationResult")
      }
      filterChain.doFilter(request, response)
    } catch (failed: AuthenticationException) {
      securityContextHolderStrategy.clearContext()
      logger.trace("Failed to process authentication request", failed)
      authenticationFailureHandler.onAuthenticationFailure(request, response, failed)
    }
  }
}
